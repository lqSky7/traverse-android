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

data class RevisionsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val revisionGroups: List<RevisionGroup> = emptyList(),
    val stats: RevisionStatsResponse? = null,
    val showCompletedRevisions: Boolean = false,
    val useMLMode: Boolean = false,
    val isFromCache: Boolean = false,
    val completingRevisionId: Int? = null,
    val deletingRevisionId: Int? = null,
    val isSubscribed: Boolean = false,
    val showProUpgradeDialog: Boolean = false
)

class RevisionsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val networkService by lazy { NetworkService.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }
    
    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()
    
    private var loadJob: Job? = null
    
    init {
        // Restore subscription status and ML mode from cache
        val isSubscribed = cacheManager.getSubscriptionStatus()
        val savedMode = cacheManager.getRevisionMode()
        _uiState.update { 
            it.copy(
                isSubscribed = isSubscribed,
                useMLMode = savedMode == "ml" && isSubscribed // Only enable ML if subscribed
            ) 
        }
        
        // Check subscription status if needed
        checkSubscriptionStatus()
        loadData()
    }
    
    private fun checkSubscriptionStatus(forceCheck: Boolean = false) {
        if (!forceCheck && !cacheManager.shouldCheckSubscription()) {
            return
        }
        
        viewModelScope.launch {
            try {
                when (val result = networkService.getSubscriptionStatus()) {
                    is NetworkResult.Success -> {
                        val isActive = result.data.isSubscriptionActive
                        cacheManager.cacheSubscriptionStatus(isActive)
                        _uiState.update { state ->
                            var newState = state.copy(isSubscribed = isActive)
                            // If ML mode is on but user is not subscribed, turn it off
                            if (state.useMLMode && !isActive) {
                                cacheManager.cacheRevisionMode("normal")
                                newState = newState.copy(useMLMode = false)
                                // Reload with normal mode
                                loadData(forceRefresh = true)
                            }
                            newState
                        }
                    }
                    is NetworkResult.Error -> {
                        // Keep using cached value on error
                    }
                }
            } catch (e: Exception) {
                // Silent failure - keep cached value
            }
        }
    }
    
    fun loadData(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val mode = if (_uiState.value.useMLMode) "ml" else "normal"
            
            // If force refresh, clear cache first
            if (forceRefresh) {
                cacheManager.invalidateRevisionCache()
            }
            
            // Try cache first if not forcing refresh
            if (!forceRefresh) {
                val cachedData = loadFromCache(mode)
                if (cachedData) {
                    refreshInBackground(mode)
                    return@launch
                }
            }
            
            loadFromNetwork(mode)
        }
    }
    
    private fun loadFromCache(mode: String): Boolean {
        val groups = cacheManager.getRevisionGroups(mode)
        val stats = cacheManager.getRevisionStats(mode)
        
        if (groups != null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    revisionGroups = groups.groups,
                    stats = stats,
                    isFromCache = true
                )
            }
            return true
        }
        return false
    }
    
    private suspend fun loadFromNetwork(mode: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val groupsDeferred = viewModelScope.async { 
                networkService.getGroupedRevisions(
                    includeCompleted = _uiState.value.showCompletedRevisions,
                    type = mode
                )
            }
            val statsDeferred = viewModelScope.async { 
                networkService.getRevisionStats(type = mode) 
            }
            
            val groupsResult = groupsDeferred.await()
            val statsResult = statsDeferred.await()
            
            // Cache successful responses
            if (groupsResult is NetworkResult.Success) {
                cacheManager.cacheRevisionGroups(groupsResult.data, mode)
            }
            if (statsResult is NetworkResult.Success) {
                cacheManager.cacheRevisionStats(statsResult.data, mode)
            }
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = (groupsResult as? NetworkResult.Error)?.message,
                    revisionGroups = (groupsResult as? NetworkResult.Success)?.data?.groups ?: emptyList(),
                    stats = (statsResult as? NetworkResult.Success)?.data,
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
    
    private fun refreshInBackground(mode: String) {
        viewModelScope.launch {
            try {
                val groupsDeferred = async { 
                    networkService.getGroupedRevisions(
                        includeCompleted = _uiState.value.showCompletedRevisions,
                        type = mode
                    )
                }
                val statsDeferred = async { 
                    networkService.getRevisionStats(type = mode) 
                }
                
                val groupsResult = groupsDeferred.await()
                val statsResult = statsDeferred.await()
                
                if (groupsResult is NetworkResult.Success) {
                    cacheManager.cacheRevisionGroups(groupsResult.data, mode)
                    _uiState.update { it.copy(revisionGroups = groupsResult.data.groups) }
                }
                if (statsResult is NetworkResult.Success) {
                    cacheManager.cacheRevisionStats(statsResult.data, mode)
                    _uiState.update { it.copy(stats = statsResult.data) }
                }
                
                _uiState.update { it.copy(isFromCache = false) }
                
            } catch (e: Exception) {
                // Silent failure for background refresh
            }
        }
    }
    
    fun toggleShowCompleted() {
        _uiState.update { it.copy(showCompletedRevisions = !it.showCompletedRevisions) }
        loadData(forceRefresh = true)
    }
    
    fun toggleMLMode() {
        val currentState = _uiState.value
        
        if (!currentState.useMLMode) {
            // User is trying to enable ML mode
            viewModelScope.launch {
                // Force check subscription status
                when (val result = networkService.getSubscriptionStatus()) {
                    is NetworkResult.Success -> {
                        val isActive = result.data.isSubscriptionActive
                        cacheManager.cacheSubscriptionStatus(isActive)
                        
                        if (isActive) {
                            // User is subscribed - enable ML mode
                            _uiState.update { it.copy(useMLMode = true, isSubscribed = true) }
                            cacheManager.cacheRevisionMode("ml")
                            loadData(forceRefresh = true)
                        } else {
                            // User is not subscribed - show upgrade dialog
                            _uiState.update { it.copy(showProUpgradeDialog = true, isSubscribed = false) }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(errorMessage = "Failed to check subscription status") }
                    }
                }
            }
        } else {
            // User is turning off ML mode - no need to check subscription
            _uiState.update { it.copy(useMLMode = false) }
            cacheManager.cacheRevisionMode("normal")
            loadData(forceRefresh = true)
        }
    }
    
    fun dismissProUpgradeDialog() {
        _uiState.update { it.copy(showProUpgradeDialog = false) }
    }
    
    fun refresh() {
        viewModelScope.launch {
            // Clear cache and reload fresh data
            cacheManager.invalidateRevisionCache()
            
            // Force network loading
            val mode = if (_uiState.value.useMLMode) "ml" else "normal"
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadFromNetwork(mode)
        }
    }
    
    fun completeRevision(revisionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(completingRevisionId = revisionId) }
            
            when (val result = networkService.completeRevision(revisionId)) {
                is NetworkResult.Success -> {
                    // Invalidate cache and reload
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
            
            _uiState.update { it.copy(completingRevisionId = null) }
        }
    }
    
    fun deleteRevision(revisionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingRevisionId = revisionId) }
            
            when (val result = networkService.deleteRevision(revisionId)) {
                is NetworkResult.Success -> {
                    // Invalidate cache and reload
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
            
            _uiState.update { it.copy(deletingRevisionId = null) }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun invalidateAllCache() {
        cacheManager.clearAllCache()
    }
}
