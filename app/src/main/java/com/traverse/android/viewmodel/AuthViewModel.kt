package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.CacheManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.PushRegistrationManager
import com.traverse.android.data.User
import com.traverse.android.ui.components.AchievementToastManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isDataLoaded: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    /**
     * One-shot: the URL the UI should hand to a browser, then clear with
     * [AuthViewModel.consumeGitHubAuthUrl]. Null the rest of the time.
     */
    val githubAuthUrl: String? = null,
    /**
     * True from the moment GitHub sign-in is requested until the deep link
     * comes back — or until the app resumes without one, which means the
     * reader abandoned it. Without this the sign-in button would stay disabled
     * for the rest of the session after a cancelled attempt.
     */
    val isGitHubSignInInProgress: Boolean = false
)

@Serializable
private data class CatApiResponse(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val networkService = NetworkService.getInstance(application)
    private val cacheManager = CacheManager.getInstance(application)
    private val toastManager = AchievementToastManager.getInstance(application)
    private val sessionCleaner = AccountSessionCleaner(application)
    private var avatarJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthentication()
    }
    
    private fun checkAuthentication() {
        val isAuthenticated = networkService.isAuthenticated()
        
        if (isAuthenticated) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true, 
                isDataLoaded = true
            )
            viewModelScope.launch {
                fetchCurrentUser()
                toastManager.syncAppOpenUpdates()
            }
        } else {
            _uiState.value = _uiState.value.copy(isAuthenticated = false)
        }
    }
    
    private fun startAvatarLoad() {
        avatarJob?.cancel()
        avatarJob = viewModelScope.launch {
            try {
                ensureProfileImage()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Optional decoration must never block signing in or loading Home.
            }
        }
    }

    private suspend fun readAvatarBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = java.net.URL(url).openConnection().apply {
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        try {
            connection.getInputStream().use { it.readBytes() }
        } finally {
            (connection as? java.net.HttpURLConnection)?.disconnect()
        }
    }

    private suspend fun ensureProfileImage() {
        val cachedFile = cacheManager.getProfileImageFile()
        if (cachedFile != null && java.io.File(cachedFile).exists()) return

        suspend fun newCatUrl(): String? {
            val response = readAvatarBytes("https://api.thecatapi.com/v1/images/search")
            return json.decodeFromString<List<CatApiResponse>>(response.toString(Charsets.UTF_8))
                .firstOrNull()?.url
        }

        var imageUrl = cacheManager.getProfileImage() ?: newCatUrl() ?: return
        val bytes = try {
            readAvatarBytes(imageUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            imageUrl = newCatUrl() ?: return
            readAvatarBytes(imageUrl)
        }
        val file = java.io.File(getApplication<Application>().filesDir,
            "profile_image_${System.currentTimeMillis()}.jpg")
        try {
            withContext(Dispatchers.IO) { file.writeBytes(bytes) }
            // Cancellation on logout prevents an old request restoring the avatar cache.
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            cacheManager.cacheProfileImage(imageUrl)
            cacheManager.cacheProfileImageFile(file.absolutePath)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private fun finishSignIn() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isAuthenticated = true,
            isDataLoaded = true
        )
        // Home owns its own fetch; neither avatars nor toast checks gate app entry.
        startAvatarLoad()
        viewModelScope.launch { toastManager.syncAppOpenUpdates(force = true) }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = networkService.login(username, password)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUser = result.data.user
                    )
                    finishSignIn()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
    
    /**
     * GitHub sign-in, part one: fetch the authorization URL and hand it to the
     * UI to open in a browser. Deliberately not a suspend function the screen
     * awaits — the browser leaves the app, and the answer comes back minutes
     * later on a completely different code path ([handleGitHubCallback]).
     */
    fun startGitHubSignIn() {
        if (_uiState.value.isGitHubSignInInProgress) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGitHubSignInInProgress = true,
                errorMessage = null
            )

            when (val result = networkService.getGitHubAuthUrl()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(githubAuthUrl = result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGitHubSignInInProgress = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /** The URL has reached the browser; clear it so it is not opened twice. */
    fun consumeGitHubAuthUrl() {
        _uiState.value = _uiState.value.copy(githubAuthUrl = null)
    }

    /**
     * GitHub sign-in, part two: the deep link came back carrying a code.
     *
     * The guard matters on configuration change — Android redelivers the
     * launching intent on recreate, and an OAuth code is single-use, so a
     * second exchange would fail with a 401 and overwrite a good session with
     * an error.
     */
    fun handleGitHubCallback(code: String) {
        val state = _uiState.value
        if (state.isLoading || state.isAuthenticated) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isGitHubSignInInProgress = false,
                githubAuthUrl = null,
                errorMessage = null
            )

            when (val result = networkService.loginWithGitHubCode(code)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUser = result.data.user
                    )
                    finishSignIn()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * The browser came back without a code — the reader pressed back, or GitHub
     * refused before redirecting. Drops the pending flag so sign-in is usable
     * again; a deep link that *did* arrive clears it first, and this then
     * no-ops.
     */
    fun onReturnedToForeground() {
        val state = _uiState.value
        if (state.isGitHubSignInInProgress && state.githubAuthUrl == null) {
            _uiState.value = state.copy(isGitHubSignInInProgress = false)
        }
    }

    /** GitHub itself refused the request, or the reader declined at its prompt. */
    fun handleGitHubError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isGitHubSignInInProgress = false,
            githubAuthUrl = null,
            errorMessage = message
        )
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            avatarJob?.cancelAndJoin()
            avatarJob = null
            PushRegistrationManager.getInstance(getApplication()).unregisterFromServer()
            networkService.logout()
            sessionCleaner.clear()
            
            _uiState.value = AuthUiState(isAuthenticated = false)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    private suspend fun fetchCurrentUser() {
        when (val result = networkService.getCurrentUser()) {
            is NetworkResult.Success -> {
                _uiState.value = _uiState.value.copy(currentUser = result.data)
                startAvatarLoad()
            }
            is NetworkResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    isDataLoaded = false,
                    currentUser = null
                )
            }
        }
    }
}
