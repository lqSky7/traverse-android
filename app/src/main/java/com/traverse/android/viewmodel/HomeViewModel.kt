package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userStats: UserStats? = null,
    val solveStats: SolveStats? = null,
    val recentSolves: List<Solve> = emptyList(),
    val achievementStats: AchievementStats? = null,
    val allAchievements: List<AchievementDetail> = emptyList(),
    val frozenDates: List<String> = emptyList(),
    val isFromCache: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val networkService by lazy { NetworkService.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Try cache first if not forcing refresh
            if (!forceRefresh) {
                val cachedData = loadFromCache()
                if (cachedData) {
                    // Data loaded from cache, fetch fresh data in background
                    refreshInBackground()
                    return@launch
                }
            }
            
            // No cache or force refresh - load from network
            loadFromNetwork()
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            // Clear cache and reload fresh data
            cacheManager.invalidateHomeCache()
            
            // Force network loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadFromNetwork()
        }
    }
    
    private fun loadFromCache(): Boolean {
        val userStats = cacheManager.getUserStats()
        val solveStats = cacheManager.getSolveStats()
        val recentSolves = cacheManager.getRecentSolves()
        val achievementStats = cacheManager.getAchievementStats()
        val allAchievements = cacheManager.getAllAchievements()
        val freezeDates = cacheManager.getFreezeDates()
        
        // Consider cache valid if we have at least user stats and solve stats
        if (userStats != null && solveStats != null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    userStats = userStats,
                    solveStats = solveStats,
                    recentSolves = recentSolves?.solves ?: emptyList(),
                    achievementStats = achievementStats,
                    allAchievements = allAchievements?.achievements ?: emptyList(),
                    frozenDates = freezeDates?.getAllDates() ?: emptyList(),
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
            val userStatsDeferred = viewModelScope.async { networkService.getUserStats() }
            val solveStatsDeferred = viewModelScope.async { networkService.getSolveStats() }
            val solvesDeferred = viewModelScope.async { networkService.getSolves(limit = 50) }
            val achievementStatsDeferred = viewModelScope.async { networkService.getAchievementStats() }
            val allAchievementsDeferred = viewModelScope.async { networkService.getAllAchievements() }
            val freezeDatesDeferred = viewModelScope.async { networkService.getFreezeDates() }
            
            val userStatsResult = userStatsDeferred.await()
            val solveStatsResult = solveStatsDeferred.await()
            val solvesResult = solvesDeferred.await()
            val achievementStatsResult = achievementStatsDeferred.await()
            val allAchievementsResult = allAchievementsDeferred.await()
            val freezeDatesResult = freezeDatesDeferred.await()
            
            // Cache successful responses
            if (userStatsResult is NetworkResult.Success) cacheManager.cacheUserStats(userStatsResult.data)
            if (solveStatsResult is NetworkResult.Success) cacheManager.cacheSolveStats(solveStatsResult.data)
            if (solvesResult is NetworkResult.Success) cacheManager.cacheRecentSolves(solvesResult.data)
            if (achievementStatsResult is NetworkResult.Success) cacheManager.cacheAchievementStats(achievementStatsResult.data)
            if (allAchievementsResult is NetworkResult.Success) cacheManager.cacheAllAchievements(allAchievementsResult.data)
            if (freezeDatesResult is NetworkResult.Success) cacheManager.cacheFreezeDates(freezeDatesResult.data)
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    userStats = (userStatsResult as? NetworkResult.Success)?.data,
                    solveStats = (solveStatsResult as? NetworkResult.Success)?.data,
                    recentSolves = (solvesResult as? NetworkResult.Success)?.data?.solves ?: emptyList(),
                    achievementStats = (achievementStatsResult as? NetworkResult.Success)?.data,
                    allAchievements = (allAchievementsResult as? NetworkResult.Success)?.data?.achievements ?: emptyList(),
                    frozenDates = (freezeDatesResult as? NetworkResult.Success)?.data?.getAllDates() ?: emptyList(),
                    isFromCache = false
                )
            }
            
            // Check for any errors
            val error = listOf(userStatsResult, solveStatsResult)
                .filterIsInstance<NetworkResult.Error>()
                .firstOrNull()
            
            if (error != null && _uiState.value.userStats == null) {
                _uiState.update { it.copy(errorMessage = error.message) }
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
                val userStatsDeferred = async { networkService.getUserStats() }
                val solveStatsDeferred = async { networkService.getSolveStats() }
                val solvesDeferred = async { networkService.getSolves(limit = 50) }
                val achievementStatsDeferred = async { networkService.getAchievementStats() }
                val allAchievementsDeferred = async { networkService.getAllAchievements() }
                val freezeDatesDeferred = async { networkService.getFreezeDates() }
                
                val userStatsResult = userStatsDeferred.await()
                val solveStatsResult = solveStatsDeferred.await()
                val solvesResult = solvesDeferred.await()
                val achievementStatsResult = achievementStatsDeferred.await()
                val allAchievementsResult = allAchievementsDeferred.await()
                val freezeDatesResult = freezeDatesDeferred.await()
                
                // Update cache and state silently
                if (userStatsResult is NetworkResult.Success) {
                    cacheManager.cacheUserStats(userStatsResult.data)
                    _uiState.update { it.copy(userStats = userStatsResult.data) }
                }
                if (solveStatsResult is NetworkResult.Success) {
                    cacheManager.cacheSolveStats(solveStatsResult.data)
                    _uiState.update { it.copy(solveStats = solveStatsResult.data) }
                }
                if (solvesResult is NetworkResult.Success) {
                    cacheManager.cacheRecentSolves(solvesResult.data)
                    _uiState.update { it.copy(recentSolves = solvesResult.data.solves) }
                }
                if (achievementStatsResult is NetworkResult.Success) {
                    cacheManager.cacheAchievementStats(achievementStatsResult.data)
                    _uiState.update { it.copy(achievementStats = achievementStatsResult.data) }
                }
                if (allAchievementsResult is NetworkResult.Success) {
                    cacheManager.cacheAllAchievements(allAchievementsResult.data)
                    _uiState.update { it.copy(allAchievements = allAchievementsResult.data.achievements) }
                }
                if (freezeDatesResult is NetworkResult.Success) {
                    cacheManager.cacheFreezeDates(freezeDatesResult.data)
                    _uiState.update { it.copy(frozenDates = freezeDatesResult.data.getAllDates()) }
                }
                
                _uiState.update { it.copy(isFromCache = false) }
                
            } catch (e: Exception) {
                // Silent failure for background refresh - we already have cached data
            }
        }
    }
    
    fun clearCache() {
        cacheManager.invalidateHomeCache()
    }
    
    fun invalidateAllCache() {
        cacheManager.clearAllCache()
    }
}
