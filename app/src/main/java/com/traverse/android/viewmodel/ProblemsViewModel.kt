package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.DataManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Solve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProblemsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val solves: List<Solve> = emptyList()
)

/**
 * Owns the deep solve payload for the Problems tab. Mirrors iOS `ProblemsViewModel`.
 *
 * Home deliberately does *not* fetch this any more: the rows carry AI analysis text, mistake tags
 * and the full attempt history, which is the heaviest thing the API returns and was being pulled on
 * every pull-to-refresh to feed two cards. This view model is where that cost now lives, and only
 * when the tab is actually opened.
 */
class ProblemsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataManager by lazy { DataManager.getInstance(application) }
    private val networkService by lazy { NetworkService.getInstance(application) }

    private val _uiState = MutableStateFlow(ProblemsUiState())
    val uiState: StateFlow<ProblemsUiState> = _uiState.asStateFlow()

    /** Guards against re-fetching the deep payload on every visit to the tab. */
    private var hasLoadedDeepPayload = false

    init {
        // Seed from the shared cache so the tab paints instantly, then let `load` decide whether a
        // network round-trip is warranted. Sharing `DataManager.recentSolves` rather than holding a
        // private copy is what keeps this list and the home charts from disagreeing.
        viewModelScope.launch {
            dataManager.recentSolves.collect { solves ->
                _uiState.update { it.copy(solves = solves) }
            }
        }

        // The sign-in flow already pulls the deep payload, and `mergeAndPersistSolves` unions every
        // fetch into the persisted cache rather than replacing it — so on any returning user this
        // list is already deeper than what the home feed just fetched. Going to the network again
        // on every tab visit would be the exact cost this tab exists to avoid. Only a genuinely
        // cold start (no persisted solves at all) fetches here; pull-to-refresh can always force it.
        if (dataManager.recentSolves.value.isEmpty()) {
            load()
        } else {
            hasLoadedDeepPayload = true
        }
    }

    fun load(forceRefresh: Boolean = false) {
        if (!forceRefresh && hasLoadedDeepPayload) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = networkService.getSolves(limit = DataManager.DEEP_SOLVE_LIMIT)) {
                is NetworkResult.Success -> {
                    // Merges into the shared cache rather than replacing it, so the home charts keep
                    // whatever deeper history has already accumulated.
                    dataManager.mergeAndPersistSolves(result.data.solves)
                    hasLoadedDeepPayload = true
                    _uiState.update { it.copy(isLoading = false) }
                }

                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
