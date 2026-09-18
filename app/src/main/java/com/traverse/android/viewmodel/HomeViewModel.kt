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
    /** Award shelves for the Awards hub, in display order. */
    val awardSections: List<AwardSection> = emptyList(),
    /** The challenge the Awards hub leads with. */
    val featuredAward: AchievementDetail? = null,
    val frozenDates: List<String> = emptyList(),
    val revisionScore: RevisionScoreResponse? = null,
    val completedRevisions: List<Revision> = emptyList(),
    /**
     * Overall load plus a per-difficulty breakdown for the Revision Load card and its detail
     * screen. Derived from the unfiltered revision list and [recentSolves], so it is recomputed
     * whenever either changes rather than fetched separately.
     */
    val revisionLoad: RevisionLoadBreakdown? = null,
    val isFromCache: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataManager by lazy { DataManager.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }

    /**
     * The two inputs the load breakdown is derived from, kept here so a change to either one can
     * recompute it. `RevisionLoadBreakdown.build` walks both lists, so recomputing it on every
     * recomposition would be wasteful; it only needs to run when the data actually moves.
     */
    private var latestSolves: List<Solve> = emptyList()
    private var latestRevisions: List<Revision> = emptyList()
    
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
                latestSolves = solves
                _uiState.update { it.copy(recentSolves = solves) }
                rebuildLoadBreakdown()
            }
        }
        viewModelScope.launch {
            dataManager.allRevisions.collect { revisions ->
                latestRevisions = revisions
                rebuildLoadBreakdown()
            }
        }
        viewModelScope.launch {
            dataManager.frozenDates.collect { dates ->
                _uiState.update { it.copy(frozenDates = dates) }
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
        viewModelScope.launch {
            dataManager.awardSections.collect { sections ->
                _uiState.update { it.copy(awardSections = sections) }
            }
        }
        viewModelScope.launch {
            dataManager.featuredAward.collect { featured ->
                _uiState.update { it.copy(featuredAward = featured) }
            }
        }
        viewModelScope.launch {
            dataManager.revisionScore.collect { score ->
                _uiState.update { it.copy(revisionScore = score) }
            }
        }
        viewModelScope.launch {
            dataManager.completedRevisions.collect { revisions ->
                _uiState.update { it.copy(completedRevisions = revisions) }
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

    /**
     * Rebuilds the load breakdown from the current inputs. Cheap enough to run on every data
     * emission, but not on every recomposition, which is why the inputs are cached rather than the
     * breakdown being computed inside the composable.
     */
    private fun rebuildLoadBreakdown() {
        val breakdown = RevisionLoadBreakdown.build(
            revisions = latestRevisions,
            solves = latestSolves
        )
        _uiState.update { it.copy(revisionLoad = breakdown) }
    }

    fun refresh() {
        val username = dataManager.userStats.value?.username ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Freeze dates are always refreshed first and fail silently (1:1 with iOS loadData).
            try {
                dataManager.fetchFreezeDates()
            } catch (_: Exception) {
                // Non-critical for display
            }
            try {
                // Deliberately the *home* limit, not the deep one. Every card on this feed reads
                // solve history, but none of them needs more than enough to fill a chart axis —
                // the deep payload with its AI blobs and attempt histories belongs to the Problems
                // tab, which fetches it on demand. See `DataManager.HOME_SOLVE_LIMIT`.
                dataManager.fetchAllData(username, solveLimit = DataManager.HOME_SOLVE_LIMIT)
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

