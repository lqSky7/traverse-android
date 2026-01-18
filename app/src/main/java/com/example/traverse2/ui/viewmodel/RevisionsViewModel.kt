package com.example.traverse2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.data.api.RevisionAttemptRequest
import com.example.traverse2.data.api.RevisionGroup
import com.example.traverse2.data.api.RevisionItem
import com.example.traverse2.data.api.RevisionStatsResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class RevisionsUiState(
    val isLoading: Boolean = true,
    val normalStats: RevisionStatsResponse? = null,
    val mlStats: RevisionStatsResponse? = null,
    val normalGroups: List<RevisionGroup> = emptyList(),
    val mlGroups: List<RevisionGroup> = emptyList(),
    val currentTab: RevisionType = RevisionType.NORMAL,
    val error: String? = null,
    val hasMLAccess: Boolean = false,
    val showPaywall: Boolean = false
)

enum class RevisionType {
    NORMAL, ML
}

class RevisionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()
    
    init {
        loadRevisions()
    }
    
    fun switchTab(type: RevisionType) {
        if (type == RevisionType.ML && !_uiState.value.hasMLAccess) {
            _uiState.value = _uiState.value.copy(showPaywall = true)
        } else {
            _uiState.value = _uiState.value.copy(currentTab = type)
        }
    }
    
    fun dismissPaywall() {
        _uiState.value = _uiState.value.copy(showPaywall = false)
    }
    
    fun loadRevisions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Fetch all revision data in parallel
                supervisorScope {
                    val normalStatsDeferred = async { 
                        runCatching { RetrofitClient.api.getRevisionStats(type = "normal") }.getOrNull() 
                    }
                    val normalGroupedDeferred = async { 
                        runCatching { RetrofitClient.api.getRevisionsGrouped(includeCompleted = false, type = "normal") }.getOrNull() 
                    }
                    val mlStatsDeferred = async { 
                        runCatching { RetrofitClient.api.getRevisionStats(type = "ml") }.getOrNull() 
                    }
                    val mlGroupedDeferred = async { 
                        runCatching { RetrofitClient.api.getRevisionsGrouped(includeCompleted = false, type = "ml") }.getOrNull() 
                    }
                    
                    // Await all results
                    val normalStatsResponse = normalStatsDeferred.await()
                    val normalGroupedResponse = normalGroupedDeferred.await()
                    val mlStatsResponse = mlStatsDeferred.await()
                    val mlGroupedResponse = mlGroupedDeferred.await()
                    
                    val hasMLAccess = mlStatsResponse?.isSuccessful == true && mlGroupedResponse?.isSuccessful == true
                    
                    if (normalStatsResponse?.isSuccessful == true && normalGroupedResponse?.isSuccessful == true) {
                        _uiState.value = RevisionsUiState(
                            isLoading = false,
                            normalStats = normalStatsResponse.body(),
                            normalGroups = normalGroupedResponse.body()?.groups ?: emptyList(),
                            mlStats = if (hasMLAccess) mlStatsResponse?.body() else null,
                            mlGroups = if (hasMLAccess) mlGroupedResponse?.body()?.groups ?: emptyList() else emptyList(),
                            hasMLAccess = hasMLAccess,
                            currentTab = _uiState.value.currentTab,
                            error = null
                        )
                    } else {
                        _uiState.value = RevisionsUiState(
                            isLoading = false,
                            normalStats = null,
                            normalGroups = emptyList(),
                            mlStats = null,
                            mlGroups = emptyList(),
                            hasMLAccess = false,
                            error = "Failed to load revisions"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RevisionsUiState(
                    isLoading = false,
                    normalStats = null,
                    normalGroups = emptyList(),
                    mlStats = null,
                    mlGroups = emptyList(),
                    hasMLAccess = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    fun completeRevision(revisionId: Int, isML: Boolean = false) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.completeRevision(revisionId)
                
                if (response.isSuccessful) {
                    loadRevisions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to complete revision: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    fun recordMLAttempt(
        revisionId: Int,
        outcome: Int,
        numTries: Int,
        timeSpentMinutes: Int
    ) {
        viewModelScope.launch {
            try {
                val request = RevisionAttemptRequest(
                    outcome = outcome,
                    numTries = numTries,
                    timeSpentMinutes = timeSpentMinutes
                )
                
                val response = RetrofitClient.api.recordRevisionAttempt(revisionId, request)
                
                if (response.isSuccessful) {
                    val result = response.body()
                    // Show prediction info if needed
                    loadRevisions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to record attempt: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
