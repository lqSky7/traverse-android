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
    val analytics: RevisionAnalyticsResponse? = null,
    val isAnalyticsLoading: Boolean = false,
    val analyticsError: String? = null,
    val showCompletedRevisions: Boolean = false,
    val useMLMode: Boolean = true,
    val isFromCache: Boolean = false,
    val completingRevisionId: Int? = null,
    val deletingRevisionId: Int? = null,
    val isSubscribed: Boolean = false,
    val showProUpgradeDialog: Boolean = false,
    val isExamModeActive: Boolean = false,
    val dailyReviewLimit: Int = 10
)

class RevisionsViewModel(application: Application) : AndroidViewModel(application) {

    private val networkService by lazy { NetworkService.getInstance(application) }
    private val dataManager by lazy { DataManager.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }

    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Observe DataManager flows for real-time reactivity
        viewModelScope.launch {
            dataManager.revisionGroups.collect { groups ->
                _uiState.update { it.copy(revisionGroups = groups) }
            }
        }
        viewModelScope.launch {
            dataManager.revisionStats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }

        // Restore subscription status, ML mode, and Exam Mode from cache
        val isSubscribed = cacheManager.getSubscriptionStatus()
        val isExamMode = cacheManager.getExamMode()

        _uiState.update {
            it.copy(
                isSubscribed = isSubscribed,
                useMLMode = true,
                isExamModeActive = isExamMode,
                showProUpgradeDialog = !isSubscribed
            )
        }

        checkSubscriptionStatus()
        loadData()
        loadAnalytics()
    }

    fun checkSubscriptionStatus(forceCheck: Boolean = false) {
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
                            state.copy(
                                isSubscribed = isActive,
                                showProUpgradeDialog = !isActive
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        // Keep using cached value on error
                    }
                }
            } catch (e: Exception) {
                // Silent failure
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val mode = "ml"

            if (forceRefresh) {
                cacheManager.invalidateRevisionCache()
            }

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

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyticsLoading = true, analyticsError = null) }

            try {
                when (val result = networkService.getRevisionAnalytics()) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                analytics = result.data,
                                isAnalyticsLoading = false,
                                analyticsError = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isAnalyticsLoading = false,
                                analyticsError = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyticsLoading = false,
                        analyticsError = e.message ?: "Failed to load analytics"
                    )
                }
            }
        }
    }

    fun dismissProUpgradeDialog() {
        _uiState.update { it.copy(showProUpgradeDialog = false) }
    }

    fun refresh() {
        viewModelScope.launch {
            cacheManager.invalidateRevisionCache()
            loadData(forceRefresh = true)
            loadAnalytics()
        }
    }

    fun completeRevision(revisionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(completingRevisionId = revisionId) }

            when (val result = networkService.completeRevision(revisionId)) {
                is NetworkResult.Success -> {
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                    loadAnalytics()
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
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                    loadAnalytics()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }

            _uiState.update { it.copy(deletingRevisionId = null) }
        }
    }

    fun deleteProblemRevisions(problemId: Int) {
        viewModelScope.launch {
            when (val result = networkService.deleteProblemRevisions(problemId)) {
                is NetworkResult.Success -> {
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                    loadAnalytics()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun rescheduleRevision(revisionId: Int, days: Int) {
        viewModelScope.launch {
            when (val result = networkService.rescheduleRevision(revisionId, days)) {
                is NetworkResult.Success -> {
                    cacheManager.invalidateRevisionCache()
                    loadData(forceRefresh = true)
                    loadAnalytics()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun setExamMode(active: Boolean) {
        cacheManager.cacheExamMode(active)
        _uiState.update { it.copy(isExamModeActive = active) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun invalidateAllCache() {
        cacheManager.clearAllCache()
    }
}
