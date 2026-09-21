package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.CacheManager
import com.traverse.android.data.DataManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.PushRegistrationManager
import com.traverse.android.data.User
import com.traverse.android.ui.components.AchievementToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.TimeZone

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
    private val dataManager = DataManager.getInstance(application)
    private val toastManager = AchievementToastManager.getInstance(application)
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
    
    private suspend fun fetchRandomCatImage(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.thecatapi.com/v1/images/search")
                val connection = url.openConnection()
                connection.setRequestProperty("Content-Type", "application/json")
                val response = connection.getInputStream().bufferedReader().readText()
                val cats = json.decodeFromString<List<CatApiResponse>>(response)
                cats.firstOrNull()?.url
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private suspend fun ensureProfileImage() {
        val cachedImageFile = cacheManager.getProfileImageFile()
        if (cachedImageFile != null && java.io.File(cachedImageFile).exists()) {
            return
        }
        
        val cachedImageUrl = cacheManager.getProfileImage()
        if (cachedImageUrl != null) {
            try {
                val imageBytes = withContext(Dispatchers.IO) {
                    val url = java.net.URL(cachedImageUrl)
                    url.readBytes()
                }
                val context = getApplication<Application>().applicationContext
                val filename = "profile_image_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, filename)
                file.writeBytes(imageBytes)
                cacheManager.cacheProfileImageFile(file.absolutePath)
            } catch (e: Exception) {
                val catImageUrl = fetchRandomCatImage()
                catImageUrl?.let { 
                    cacheManager.cacheProfileImage(it)
                    try {
                        val imageBytes = withContext(Dispatchers.IO) {
                            val url = java.net.URL(it)
                            url.readBytes()
                        }
                        val context = getApplication<Application>().applicationContext
                        val filename = "profile_image_${System.currentTimeMillis()}.jpg"
                        val file = java.io.File(context.filesDir, filename)
                        file.writeBytes(imageBytes)
                        cacheManager.cacheProfileImageFile(file.absolutePath)
                    } catch (_: Exception) {}
                }
            }
            return
        }
        
        val catImageUrl = fetchRandomCatImage()
        catImageUrl?.let { url ->
            cacheManager.cacheProfileImage(url)
            try {
                val imageBytes = withContext(Dispatchers.IO) {
                    val netUrl = java.net.URL(url)
                    netUrl.readBytes()
                }
                val context = getApplication<Application>().applicationContext
                val filename = "profile_image_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, filename)
                file.writeBytes(imageBytes)
                cacheManager.cacheProfileImageFile(file.absolutePath)
            } catch (_: Exception) {}
        }
    }
    
    private suspend fun preloadAllData(username: String) {
        try {
            dataManager.fetchAllData(username)
            ensureProfileImage()
            toastManager.syncAppOpenUpdates(force = true)
        } catch (_: Exception) {
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isAuthenticated = true,
            isDataLoaded = true
        )
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
                    preloadAllData(result.data.user.username)
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
    
    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val timezone = TimeZone.getDefault().id
            
            when (val result = networkService.register(username, email, password, timezone)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUser = result.data.user
                    )
                    preloadAllData(result.data.user.username)
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
                    preloadAllData(result.data.user.username)
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
            
            PushRegistrationManager.getInstance(getApplication()).unregisterFromServer()
            networkService.logout()
            cacheManager.clearAllCache()
            dataManager.clearAllData()
            toastManager.resetState()
            
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
                dataManager.fetchAllData(result.data.username)
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
