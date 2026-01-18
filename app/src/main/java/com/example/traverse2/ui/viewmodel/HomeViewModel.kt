package com.example.traverse2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traverse2.data.api.Achievement
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.data.api.Solve
import com.example.traverse2.data.api.SolveStats
import com.example.traverse2.data.api.SubmissionStats
import com.example.traverse2.data.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val user: User? = null,
    val solveStats: SolveStats? = null,
    val submissionStats: SubmissionStats? = null,
    val recentSolves: List<Solve> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val calendarSolveDates: Set<LocalDate> = emptySet()
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
                // Fetch all data in parallel using supervisorScope
                // This allows individual failures without canceling other requests
                supervisorScope {
                    // Critical: User data (required)
                    val userDeferred = async { RetrofitClient.api.getCurrentUser() }
                    
                    // Non-critical: Stats and other data (can fail gracefully)
                    val solveStatsDeferred = async { 
                        runCatching { RetrofitClient.api.getSolveStats() }.getOrNull() 
                    }
                    val submissionStatsDeferred = async { 
                        runCatching { RetrofitClient.api.getSubmissionStats() }.getOrNull() 
                    }
                    val recentSolvesDeferred = async { 
                        runCatching { RetrofitClient.api.getMySolves(limit = 4, offset = 0) }.getOrNull() 
                    }
                    val achievementsDeferred = async { 
                        runCatching { RetrofitClient.api.getAchievements() }.getOrNull() 
                    }
                    
                    // Calendar data (parallel fetch for solves and revisions)
                    val calendarSolvesDeferred = async {
                        runCatching {
                            val response = RetrofitClient.api.getMySolves(limit = 100, offset = 0)
                            if (response.isSuccessful) {
                                response.body()?.solves?.mapNotNull { solve ->
                                    try {
                                        Instant.parse(solve.solvedAt)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                    } catch (e: Exception) { null }
                                }?.toSet() ?: emptySet()
                            } else emptySet()
                        }.getOrDefault(emptySet())
                    }
                    
                    val calendarRevisionsDeferred = async {
                        runCatching {
                            val response = RetrofitClient.api.getRevisionsGrouped(includeCompleted = true)
                            if (response.isSuccessful) {
                                response.body()?.groups?.flatMap { it.revisions }
                                    ?.filter { it.completedAt != null }
                                    ?.mapNotNull { revision ->
                                        try {
                                            Instant.parse(revision.completedAt!!)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        } catch (e: Exception) { null }
                                    }?.toSet() ?: emptySet()
                            } else emptySet()
                        }.getOrDefault(emptySet())
                    }
                    
                    // Await user data first (critical)
                    val userResponse = userDeferred.await()
                    if (!userResponse.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load user data"
                        )
                        return@supervisorScope
                    }
                    val user = userResponse.body()!!.user
                    
                    // Await all other results (already running in parallel)
                    val solveStatsResponse = solveStatsDeferred.await()
                    val submissionStatsResponse = submissionStatsDeferred.await()
                    val recentSolvesResponse = recentSolvesDeferred.await()
                    val achievementsResponse = achievementsDeferred.await()
                    val solveDates = calendarSolvesDeferred.await()
                    val revisionDates = calendarRevisionsDeferred.await()
                    
                    // Extract data with null-safety
                    val solveStats = solveStatsResponse?.body()?.stats
                    val submissionStats = submissionStatsResponse?.body()?.stats
                    val recentSolves = recentSolvesResponse?.body()?.solves ?: emptyList()
                    val achievements = achievementsResponse?.body()?.achievements ?: emptyList()
                    val calendarSolveDates = solveDates + revisionDates
                    
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        error = null,
                        user = user,
                        solveStats = solveStats,
                        submissionStats = submissionStats,
                        recentSolves = recentSolves,
                        achievements = achievements,
                        calendarSolveDates = calendarSolveDates
                    )
                }
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
