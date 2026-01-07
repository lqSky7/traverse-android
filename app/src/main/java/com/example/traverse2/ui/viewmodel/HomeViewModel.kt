package com.example.traverse2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traverse2.data.api.Achievement
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.data.api.Solve
import com.example.traverse2.data.api.SolveStats
import com.example.traverse2.data.api.SubmissionStats
import com.example.traverse2.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val user: User? = null,
    val solveStats: SolveStats? = null,
    val submissionStats: SubmissionStats? = null,
    val recentSolves: List<Solve> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Fetch user data
                val userResponse = RetrofitClient.api.getCurrentUser()
                if (!userResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load user data"
                    )
                    return@launch
                }
                val user = userResponse.body()!!.user
                
                // Fetch solve stats
                val solveStatsResponse = RetrofitClient.api.getSolveStats()
                val solveStats = if (solveStatsResponse.isSuccessful) {
                    solveStatsResponse.body()?.stats
                } else null
                
                // Fetch submission stats
                val submissionStatsResponse = RetrofitClient.api.getSubmissionStats()
                val submissionStats = if (submissionStatsResponse.isSuccessful) {
                    submissionStatsResponse.body()?.stats
                } else null
                
                // Fetch recent solves (limit 4)
                val recentSolvesResponse = RetrofitClient.api.getMySolves(limit = 4, offset = 0)
                val recentSolves = if (recentSolvesResponse.isSuccessful) {
                    recentSolvesResponse.body()?.solves ?: emptyList()
                } else emptyList()
                
                // Fetch achievements
                val achievementsResponse = RetrofitClient.api.getAchievements()
                val achievements = if (achievementsResponse.isSuccessful) {
                    achievementsResponse.body()?.achievements ?: emptyList()
                } else emptyList()
                
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = null,
                    user = user,
                    solveStats = solveStats,
                    submissionStats = submissionStats,
                    recentSolves = recentSolves,
                    achievements = achievements
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun refresh() {
        loadHomeData()
    }
}
