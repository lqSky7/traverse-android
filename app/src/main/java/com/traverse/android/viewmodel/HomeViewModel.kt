package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.*
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
    
    private val dataManager by lazy { DataManager.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        // Observe DataManager flows for real-time reactivity
        viewModelScope.launch {
            dataManager.userStats.collect { stats ->
                _uiState.update { it.copy(userStats = stats) }
            }
        }
        viewModelScope.launch {
            dataManager.solveStats.collect { stats ->
                _uiState.update { it.copy(solveStats = stats) }
            }
        }
        viewModelScope.launch {
            dataManager.recentSolves.collect { solves ->
                _uiState.update { it.copy(recentSolves = solves) }
            }
        }
        viewModelScope.launch {
            dataManager.achievementStats.collect { stats ->
                _uiState.update { it.copy(achievementStats = stats) }
            }
        }
        viewModelScope.launch {
            dataManager.allAchievements.collect { achievements ->
                _uiState.update { it.copy(allAchievements = achievements) }
            }
        }

        loadData()
    }
    
    fun loadData(forceRefresh: Boolean = false) {
        val username = dataManager.userStats.value?.username ?: ""
        if (!forceRefresh && dataManager.hasData) {
            return
        }
        refresh()
    }
    
    fun refresh() {
        val username = dataManager.userStats.value?.username ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                dataManager.fetchAllData(username)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    fun clearCache() {
        dataManager.clearAllData()
        cacheManager.invalidateHomeCache()
    }
    
    fun invalidateAllCache() {
        dataManager.clearAllData()
        cacheManager.clearAllCache()
    }
}

