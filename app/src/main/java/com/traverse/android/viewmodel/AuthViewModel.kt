package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.CacheManager
import com.traverse.android.data.DataManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
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
    val errorMessage: String? = null
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
    
    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
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

