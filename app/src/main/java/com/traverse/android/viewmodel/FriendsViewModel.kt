package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val friends: List<Friend> = emptyList(),
    val receivedRequests: List<FriendRequest> = emptyList(),
    val sentRequests: List<FriendRequest> = emptyList(),
    val isFromCache: Boolean = false,
    // Friend Streaks
    val friendStreaks: List<FriendStreak> = emptyList(),
    val receivedStreakRequests: List<FriendStreakRequest> = emptyList(),
    val sentStreakRequests: List<FriendStreakRequest> = emptyList(),
    // UI state
    val showRequestsSheet: Boolean = false,
    val showSearchSheet: Boolean = false,
    val processingRequestId: Int? = null,
    val removingFriendUsername: String? = null
)

/**
 * Get top 3 friends for leaderboard, sorted by weighted score (streak has more weight)
 */
fun FriendsUiState.getLeaderboard(): List<Friend> {
    return friends
        .sortedByDescending { friend -> friend.currentStreak * 10 + friend.totalXp / 100 }
        .take(3)
}

/**
 * Get total pending requests count (friends + streaks)
 */
fun FriendsUiState.getTotalPendingCount(): Int {
    return receivedRequests.size + receivedStreakRequests.size
}

/**
 * Get friend streak for a specific username
 */
fun FriendsUiState.getFriendStreakCount(username: String): Int? {
    return friendStreaks.find { it.friend.username == username }?.currentStreak
}

class FriendsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val networkService by lazy { NetworkService.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }
    
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    
    private var loadJob: Job? = null
    
    init {
        loadData()
    }
    
    fun loadData(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // If force refresh, clear cache first
            if (forceRefresh) {
                cacheManager.invalidateFriendsCache()
            }
            
            // Try cache first if not forcing refresh
            if (!forceRefresh) {
                val cachedData = loadFromCache()
                if (cachedData) {
                    refreshInBackground()
                    return@launch
                }
            }
            
            loadFromNetwork()
        }
    }
    
    private fun loadFromCache(): Boolean {
        val friends = cacheManager.getFriends()
        val receivedRequests = cacheManager.getReceivedRequests()
        val sentRequests = cacheManager.getSentRequests()
        
        if (friends != null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    friends = friends.friends,
                    receivedRequests = receivedRequests?.requests ?: emptyList(),
                    sentRequests = sentRequests?.requests ?: emptyList(),
                    isFromCache = true
                )
            }
            return true
        }
        return false
    }
    
    private suspend fun loadFromNetwork() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val friendsDeferred = viewModelScope.async { networkService.getFriends() }
            val receivedDeferred = viewModelScope.async { networkService.getReceivedFriendRequests() }
            val sentDeferred = viewModelScope.async { networkService.getSentFriendRequests() }
            val friendStreaksDeferred = viewModelScope.async { networkService.getFriendStreaks() }
            val receivedStreakDeferred = viewModelScope.async { networkService.getReceivedFriendStreakRequests() }
            val sentStreakDeferred = viewModelScope.async { networkService.getSentFriendStreakRequests() }
            
            val friendsResult = friendsDeferred.await()
            val receivedResult = receivedDeferred.await()
            val sentResult = sentDeferred.await()
            val friendStreaksResult = friendStreaksDeferred.await()
            val receivedStreakResult = receivedStreakDeferred.await()
            val sentStreakResult = sentStreakDeferred.await()
            
            // Cache successful responses
            if (friendsResult is NetworkResult.Success) {
                cacheManager.cacheFriends(friendsResult.data)
            }
            if (receivedResult is NetworkResult.Success) {
                cacheManager.cacheReceivedRequests(receivedResult.data)
            }
            if (sentResult is NetworkResult.Success) {
                cacheManager.cacheSentRequests(sentResult.data)
            }
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = (friendsResult as? NetworkResult.Error)?.message,
                    friends = (friendsResult as? NetworkResult.Success)?.data?.friends ?: emptyList(),
                    receivedRequests = (receivedResult as? NetworkResult.Success)?.data?.requests ?: emptyList(),
                    sentRequests = (sentResult as? NetworkResult.Success)?.data?.requests ?: emptyList(),
                    friendStreaks = (friendStreaksResult as? NetworkResult.Success)?.data?.streaks ?: emptyList(),
                    receivedStreakRequests = (receivedStreakResult as? NetworkResult.Success)?.data?.requests ?: emptyList(),
                    sentStreakRequests = (sentStreakResult as? NetworkResult.Success)?.data?.requests ?: emptyList(),
                    isFromCache = false
                )
            }
            
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    private fun refreshInBackground() {
        viewModelScope.launch {
            try {
                val friendsDeferred = async { networkService.getFriends() }
                val receivedDeferred = async { networkService.getReceivedFriendRequests() }
                val sentDeferred = async { networkService.getSentFriendRequests() }
                val friendStreaksDeferred = async { networkService.getFriendStreaks() }
                val receivedStreakDeferred = async { networkService.getReceivedFriendStreakRequests() }
                val sentStreakDeferred = async { networkService.getSentFriendStreakRequests() }
                
                val friendsResult = friendsDeferred.await()
                val receivedResult = receivedDeferred.await()
                val sentResult = sentDeferred.await()
                val friendStreaksResult = friendStreaksDeferred.await()
                val receivedStreakResult = receivedStreakDeferred.await()
                val sentStreakResult = sentStreakDeferred.await()
                
                if (friendsResult is NetworkResult.Success) {
                    cacheManager.cacheFriends(friendsResult.data)
                    _uiState.update { it.copy(friends = friendsResult.data.friends) }
                }
                if (receivedResult is NetworkResult.Success) {
                    cacheManager.cacheReceivedRequests(receivedResult.data)
                    _uiState.update { it.copy(receivedRequests = receivedResult.data.requests) }
                }
                if (sentResult is NetworkResult.Success) {
                    cacheManager.cacheSentRequests(sentResult.data)
                    _uiState.update { it.copy(sentRequests = sentResult.data.requests) }
                }
                if (friendStreaksResult is NetworkResult.Success) {
                    _uiState.update { it.copy(friendStreaks = friendStreaksResult.data.streaks) }
                }
                if (receivedStreakResult is NetworkResult.Success) {
                    _uiState.update { it.copy(receivedStreakRequests = receivedStreakResult.data.requests) }
                }
                if (sentStreakResult is NetworkResult.Success) {
                    _uiState.update { it.copy(sentStreakRequests = sentStreakResult.data.requests) }
                }
            } catch (_: Exception) {
                // Silent fail for background refresh
            }
        }
    }
    
    fun refresh() = loadData(forceRefresh = true)
    
    // MARK: - UI Actions
    
    fun showRequestsSheet() {
        _uiState.update { it.copy(showRequestsSheet = true) }
    }
    
    fun hideRequestsSheet() {
        _uiState.update { it.copy(showRequestsSheet = false) }
    }
    
    fun showSearchSheet() {
        _uiState.update { it.copy(showSearchSheet = true) }
    }
    
    fun hideSearchSheet() {
        _uiState.update { it.copy(showSearchSheet = false) }
    }
    
    // MARK: - Friend Request Actions
    
    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.acceptFriendRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Refresh data to get updated friends list
                    loadData(forceRefresh = true)
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun rejectRequest(request: FriendRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.rejectFriendRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            receivedRequests = state.receivedRequests.filter { it.id != request.id },
                            processingRequestId = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun cancelRequest(request: FriendRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.cancelFriendRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            sentRequests = state.sentRequests.filter { it.id != request.id },
                            processingRequestId = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun removeFriend(friend: Friend) {
        viewModelScope.launch {
            _uiState.update { it.copy(removingFriendUsername = friend.username) }
            
            when (val result = networkService.removeFriend(friend.username)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            friends = state.friends.filter { it.username != friend.username },
                            removingFriendUsername = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            removingFriendUsername = null
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Friend Streak Actions
    
    fun sendFriendStreakRequest(username: String) {
        viewModelScope.launch {
            when (val result = networkService.sendFriendStreakRequest(SendFriendStreakRequestBody(username))) {
                is NetworkResult.Success -> {
                    // Add to sent requests
                    _uiState.update { state ->
                        state.copy(
                            sentStreakRequests = state.sentStreakRequests + result.data.request
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }
    
    fun acceptStreakRequest(request: FriendStreakRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.acceptFriendStreakRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Remove from requests and add to streaks
                    _uiState.update { state ->
                        state.copy(
                            receivedStreakRequests = state.receivedStreakRequests.filter { it.id != request.id },
                            friendStreaks = state.friendStreaks + result.data.streak,
                            processingRequestId = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun rejectStreakRequest(request: FriendStreakRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.rejectFriendStreakRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            receivedStreakRequests = state.receivedStreakRequests.filter { it.id != request.id },
                            processingRequestId = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun cancelStreakRequest(request: FriendStreakRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestId = request.id) }
            
            when (val result = networkService.cancelFriendStreakRequest(request.id)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            sentStreakRequests = state.sentStreakRequests.filter { it.id != request.id },
                            processingRequestId = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            errorMessage = result.message,
                            processingRequestId = null
                        )
                    }
                }
            }
        }
    }
    
    fun deleteFriendStreak(username: String) {
        viewModelScope.launch {
            when (val result = networkService.deleteFriendStreak(username)) {
                is NetworkResult.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            friendStreaks = state.friendStreaks.filter { it.friend.username != username }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
