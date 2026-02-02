package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.CacheManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.User
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
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthentication()
    }
    
    private fun checkAuthentication() {
        val isAuthenticated = networkService.isAuthenticated()
        
        if (isAuthenticated) {
            // Already authenticated - ViewModels will load data from cache
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true, 
                isDataLoaded = true
            )
            viewModelScope.launch {
                fetchCurrentUser()
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
        // Check if we already have a cached profile image file
        val cachedImageFile = cacheManager.getProfileImageFile()
        if (cachedImageFile != null && java.io.File(cachedImageFile).exists()) {
            return // Already have local image file
        }
        
        // Check if we have a URL but no local file
        val cachedImageUrl = cacheManager.getProfileImage()
        if (cachedImageUrl != null) {
            // Download and save locally
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
                // If download fails, fetch new cat image
                val catImageUrl = fetchRandomCatImage()
                catImageUrl?.let { 
                    cacheManager.cacheProfileImage(it)
                    // Try to download and save this one
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
                    } catch (downloadException: Exception) {
                        // Keep the URL fallback if local save fails
                    }
                }
            }
            return
        }
        
        // No cached image at all - fetch a random cat image and cache it
        val catImageUrl = fetchRandomCatImage()
        catImageUrl?.let { url ->
            cacheManager.cacheProfileImage(url)
            // Try to download and save locally
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
            } catch (e: Exception) {
                // Keep the URL fallback if local save fails
            }
        }
    }
    
    private suspend fun preloadAllData() {
        try {
            // Load all data in parallel
            val userStatsResult = networkService.getUserStats()
            val solveStatsResult = networkService.getSolveStats()
            val recentSolvesResult = networkService.getSolves(limit = 50)
            val achievementStatsResult = networkService.getAchievementStats()
            val allAchievementsResult = networkService.getAllAchievements()
            val freezeDatesResult = networkService.getFreezeDates()
            
            // Cache successful results for Home tab
            if (userStatsResult is NetworkResult.Success) {
                cacheManager.cacheUserStats(userStatsResult.data)
            }
            if (solveStatsResult is NetworkResult.Success) {
                cacheManager.cacheSolveStats(solveStatsResult.data)
            }
            if (recentSolvesResult is NetworkResult.Success) {
                cacheManager.cacheRecentSolves(recentSolvesResult.data)
            }
            if (achievementStatsResult is NetworkResult.Success) {
                cacheManager.cacheAchievementStats(achievementStatsResult.data)
            }
            if (allAchievementsResult is NetworkResult.Success) {
                cacheManager.cacheAllAchievements(allAchievementsResult.data)
            }
            if (freezeDatesResult is NetworkResult.Success) {
                cacheManager.cacheFreezeDates(freezeDatesResult.data)
            }
            
            // Revisions tab data
            val normalRevisionsResult = networkService.getGroupedRevisions(
                includeCompleted = false,
                type = "normal"
            )
            val normalStatsResult = networkService.getRevisionStats(type = "normal")
            
            if (normalRevisionsResult is NetworkResult.Success) {
                cacheManager.cacheRevisionGroups(normalRevisionsResult.data, "normal")
            }
            if (normalStatsResult is NetworkResult.Success) {
                cacheManager.cacheRevisionStats(normalStatsResult.data, "normal")
            }
            
            // Check subscription and load ML data if needed
            val subscriptionResult = networkService.getSubscriptionStatus()
            if (subscriptionResult is NetworkResult.Success) {
                cacheManager.cacheSubscriptionStatus(subscriptionResult.data.isSubscriptionActive)
                
                if (subscriptionResult.data.isSubscriptionActive) {
                    val mlRevisionsResult = networkService.getGroupedRevisions(
                        includeCompleted = false,
                        type = "ml"
                    )
                    val mlStatsResult = networkService.getRevisionStats(type = "ml")
                    
                    if (mlRevisionsResult is NetworkResult.Success) {
                        cacheManager.cacheRevisionGroups(mlRevisionsResult.data, "ml")
                    }
                    if (mlStatsResult is NetworkResult.Success) {
                        cacheManager.cacheRevisionStats(mlStatsResult.data, "ml")
                    }
                }
            }
            
            // Friends tab data
            val friendsResult = networkService.getFriends()
            val receivedRequestsResult = networkService.getReceivedFriendRequests()
            val sentRequestsResult = networkService.getSentFriendRequests()
            
            if (friendsResult is NetworkResult.Success) {
                cacheManager.cacheFriends(friendsResult.data)
            }
            if (receivedRequestsResult is NetworkResult.Success) {
                cacheManager.cacheReceivedRequests(receivedRequestsResult.data)
            }
            if (sentRequestsResult is NetworkResult.Success) {
                cacheManager.cacheSentRequests(sentRequestsResult.data)
            }
            
            // Profile image
            ensureProfileImage()
            
        } catch (e: Exception) {
            // Even if some calls fail, proceed with what we have
        }
        
        // Mark as authenticated and data loaded
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
                    // Preload all data before showing main UI
                    preloadAllData()
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
                    // Preload all data before showing main UI
                    preloadAllData()
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
            }
            is NetworkResult.Error -> {
                // Token might be invalid, logout
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    isDataLoaded = false,
                    currentUser = null
                )
            }
        }
    }
}
