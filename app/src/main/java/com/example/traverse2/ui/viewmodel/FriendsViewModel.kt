package com.example.traverse2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traverse2.data.api.FriendAchievementItem
import com.example.traverse2.data.api.FriendItem
import com.example.traverse2.data.api.FriendSolveItem
import com.example.traverse2.data.api.FriendStatsResponse
import com.example.traverse2.data.api.FriendStreakItem
import com.example.traverse2.data.api.ReceivedFriendRequest
import com.example.traverse2.data.api.ReceivedStreakRequest
import com.example.traverse2.data.api.SentFriendRequest
import com.example.traverse2.data.api.SentStreakRequest
import com.example.traverse2.data.api.UserSearchResult
import com.example.traverse2.data.repository.FriendsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

// Main UI State for Friends screen
data class FriendsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val friends: List<FriendItem> = emptyList(),
    val receivedRequests: List<ReceivedFriendRequest> = emptyList(),
    val sentRequests: List<SentFriendRequest> = emptyList(),
    val friendStreaks: List<FriendStreakItem> = emptyList(),
    val receivedStreakRequests: List<ReceivedStreakRequest> = emptyList(),
    val sentStreakRequests: List<SentStreakRequest> = emptyList(),
    // Search
    val searchQuery: String = "",
    val searchResults: List<UserSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    // Action states
    val actionInProgress: Boolean = false,
    val actionMessage: String? = null
)

// Friend Profile State
data class FriendProfileState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val username: String = "",
    val stats: FriendStatsResponse? = null,
    val solves: List<FriendSolveItem> = emptyList(),
    val achievements: List<FriendAchievementItem> = emptyList()
)

class FriendsViewModel : ViewModel() {

    private val repository = FriendsRepository()

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _friendProfileState = MutableStateFlow(FriendProfileState())
    val friendProfileState: StateFlow<FriendProfileState> = _friendProfileState.asStateFlow()

    init {
        loadFriendsData()
    }

    fun loadFriendsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load all data in parallel using supervisorScope
                supervisorScope {
                    val friendsDeferred = async { repository.getFriends() }
                    val receivedDeferred = async { repository.getReceivedFriendRequests() }
                    val sentDeferred = async { repository.getSentFriendRequests() }
                    val streaksDeferred = async { repository.getFriendStreaks() }
                    val receivedStreakDeferred = async { repository.getReceivedStreakRequests() }
                    val sentStreakDeferred = async { repository.getSentStreakRequests() }
                    
                    // Await all results (already running in parallel)
                    val friends = friendsDeferred.await().getOrDefault(emptyList())
                    val received = receivedDeferred.await().getOrDefault(emptyList())
                    val sent = sentDeferred.await().getOrDefault(emptyList())
                    val streaks = streaksDeferred.await().getOrDefault(emptyList())
                    val receivedStreakRequests = receivedStreakDeferred.await().getOrDefault(emptyList())
                    val sentStreakRequests = sentStreakDeferred.await().getOrDefault(emptyList())

                    _uiState.value = FriendsUiState(
                        isLoading = false,
                        friends = friends,
                        receivedRequests = received,
                        sentRequests = sent,
                        friendStreaks = streaks,
                        receivedStreakRequests = receivedStreakRequests,
                        sentStreakRequests = sentStreakRequests
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load friends data"
                )
            }
        }
    }

    // ========== FRIEND REQUESTS ==========

    fun sendFriendRequest(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, actionMessage = null)

            val result = repository.sendFriendRequest(username)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = false,
                    actionMessage = "Friend request sent to $username"
                )
                loadFriendsData()
            } else {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send request"
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: Int) {
        viewModelScope.launch {
            // Find the request to get requester info for optimistic update
            val request = _uiState.value.receivedRequests.find { it.id == requestId }
            
            // Optimistic update - immediately remove from pending requests
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    receivedRequests = state.receivedRequests.filter { it.id != requestId }
                )
            }

            val result = repository.acceptFriendRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request accepted"
                    )
                }
                // Refresh to get updated friends list
                loadFriendsData()
            } else {
                // Rollback on failure - restore the request
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        receivedRequests = if (request != null) 
                            state.receivedRequests + request 
                        else 
                            state.receivedRequests
                    )
                }
            }
        }
    }

    fun rejectFriendRequest(requestId: Int) {
        viewModelScope.launch {
            // Save for potential rollback
            val request = _uiState.value.receivedRequests.find { it.id == requestId }
            
            // Optimistic update - immediately remove from pending requests
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    receivedRequests = state.receivedRequests.filter { it.id != requestId }
                )
            }

            val result = repository.rejectFriendRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request rejected"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        receivedRequests = if (request != null) 
                            state.receivedRequests + request 
                        else 
                            state.receivedRequests
                    )
                }
            }
        }
    }

    fun cancelFriendRequest(requestId: Int) {
        viewModelScope.launch {
            // Save for potential rollback
            val request = _uiState.value.sentRequests.find { it.id == requestId }
            
            // Optimistic update - immediately remove from sent requests
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    sentRequests = state.sentRequests.filter { it.id != requestId }
                )
            }

            val result = repository.cancelFriendRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request cancelled"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        sentRequests = if (request != null) 
                            state.sentRequests + request 
                        else 
                            state.sentRequests
                    )
                }
            }
        }
    }

    fun removeFriend(username: String) {
        viewModelScope.launch {
            // Save for potential rollback
            val friend = _uiState.value.friends.find { it.username == username }
            
            // Optimistic update - immediately remove from friends list
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    friends = state.friends.filter { it.username != username }
                )
            }

            val result = repository.removeFriend(username)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Removed $username from friends"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        friends = if (friend != null) 
                            state.friends + friend 
                        else 
                            state.friends
                    )
                }
            }
        }
    }

    // ========== FRIEND STREAKS ==========

    fun sendStreakRequest(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, actionMessage = null)

            val result = repository.sendStreakRequest(username)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = false,
                    actionMessage = "Streak request sent to $username"
                )
                loadFriendsData()
            } else {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send streak request"
                )
            }
        }
    }

    fun acceptStreakRequest(requestId: Int) {
        viewModelScope.launch {
            // Save for potential rollback
            val request = _uiState.value.receivedStreakRequests.find { it.id == requestId }
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    receivedStreakRequests = state.receivedStreakRequests.filter { it.id != requestId }
                )
            }

            val result = repository.acceptStreakRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Streak request accepted"
                    )
                }
                loadFriendsData()
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        receivedStreakRequests = if (request != null) 
                            state.receivedStreakRequests + request 
                        else 
                            state.receivedStreakRequests
                    )
                }
            }
        }
    }

    fun rejectStreakRequest(requestId: Int) {
        viewModelScope.launch {
            // Save for potential rollback
            val request = _uiState.value.receivedStreakRequests.find { it.id == requestId }
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    receivedStreakRequests = state.receivedStreakRequests.filter { it.id != requestId }
                )
            }

            val result = repository.rejectStreakRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Streak request rejected"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        receivedStreakRequests = if (request != null) 
                            state.receivedStreakRequests + request 
                        else 
                            state.receivedStreakRequests
                    )
                }
            }
        }
    }

    fun cancelStreakRequest(requestId: Int) {
        viewModelScope.launch {
            // Save for potential rollback
            val request = _uiState.value.sentStreakRequests.find { it.id == requestId }
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    sentStreakRequests = state.sentStreakRequests.filter { it.id != requestId }
                )
            }

            val result = repository.cancelStreakRequest(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Streak request cancelled"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        sentStreakRequests = if (request != null) 
                            state.sentStreakRequests + request 
                        else 
                            state.sentStreakRequests
                    )
                }
            }
        }
    }

    fun deleteFriendStreak(username: String) {
        viewModelScope.launch {
            // Save for potential rollback
            val streak = _uiState.value.friendStreaks.find { it.friend.username == username }
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    actionInProgress = true,
                    friendStreaks = state.friendStreaks.filter { it.friend.username != username }
                )
            }

            val result = repository.deleteFriendStreak(username)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        actionMessage = "Streak with $username ended"
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        error = result.exceptionOrNull()?.message,
                        friendStreaks = if (streak != null) 
                            state.friendStreaks + streak 
                        else 
                            state.friendStreaks
                    )
                }
            }
        }
    }

    // ========== USER SEARCH ==========

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)

            val result = repository.searchUsers(query)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = emptyList()
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    // ========== FRIEND PROFILE ==========

    fun loadFriendProfile(username: String) {
        viewModelScope.launch {
            _friendProfileState.value = FriendProfileState(isLoading = true, username = username)

            try {
                // Load stats
                val statsResult = repository.getFriendStats(username)
                val stats = statsResult.getOrNull()

                // Load recent solves
                val solvesResult = repository.getFriendSolves(username, limit = 10)
                val solves = solvesResult.getOrDefault(
                    com.example.traverse2.data.api.FriendSolvesResponse(emptyList(),
                        com.example.traverse2.data.api.PaginationInfo(0, 10, 0))
                ).solves

                // Load achievements
                val achievementsResult = repository.getFriendAchievements(username)
                val achievements = achievementsResult.getOrDefault(
                    com.example.traverse2.data.api.FriendAchievementsResponse(emptyList())
                ).achievements

                _friendProfileState.value = FriendProfileState(
                    isLoading = false,
                    username = username,
                    stats = stats,
                    solves = solves,
                    achievements = achievements
                )
            } catch (e: Exception) {
                _friendProfileState.value = _friendProfileState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load friend profile"
                )
            }
        }
    }

    // ========== UTILITY ==========

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun refresh() {
        loadFriendsData()
    }
}
