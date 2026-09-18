package com.traverse.android.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Solve
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.home.AllSolvesScreen
import com.traverse.android.ui.home.ErrorView
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 30

/**
 * A friend's solve history, rendered with the same [AllSolvesScreen] the home feed uses.
 *
 * All of the presentation lives in [AllSolvesScreen] — the same search field, topic
 * filter and `SolveRow` the home feed uses. This screen only fetches, so there is
 * exactly one solve list in the app to keep working, and a change to it shows up on
 * the profile for free.
 *
 * It replaces the inline `SolvesListView` the profile used to embed, which was a
 * second, thinner list that had its own model and its own bugs.
 *
 * Mirrors iOS `ProfileSolvesView`.
 */
@Composable
fun ProfileSolvesScreen(
    username: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    val scope = rememberCoroutineScope()

    var solves by remember { mutableStateOf<List<Solve>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<Int?>(null) }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // De-duplicates on append. A row can legitimately repeat across pages if a new
    // solve lands while the reader is paging, and a repeated `id` in a `LazyColumn`
    // key is a hard crash rather than a cosmetic glitch.
    fun append(incoming: List<Solve>) {
        if (solves.isEmpty()) {
            solves = incoming
            return
        }
        val seen = solves.map { it.id }.toSet()
        solves = solves + incoming.filter { it.id !in seen }
    }

    suspend fun loadNextPage() {
        if (isLoadingMore) return

        // The friend feed is cursor-paginated — it grows at the front, so an offset
        // would shift under the reader and pages would duplicate or skip rows.
        val isFirstPage = solves.isEmpty()
        if (!isFirstPage) isLoadingMore = true
        try {
            val result = networkService.getFriendSolves(
                username = username,
                limit = PAGE_SIZE,
                cursor = if (isFirstPage) null else nextCursor
            )
            when (result) {
                is NetworkResult.Success -> {
                    append(result.data.solves)
                    nextCursor = result.data.pagination.nextCursor
                    hasMore = nextCursor != null
                }
                is NetworkResult.Error -> {
                    // Only surface an error when there is nothing on screen; a failed
                    // *next page* should not blank out the rows already read.
                    if (solves.isEmpty()) errorMessage = result.message
                }
            }
        } finally {
            isLoadingMore = false
        }
    }

    suspend fun reload() {
        if (solves.isEmpty()) isLoading = true
        errorMessage = null
        nextCursor = null
        hasMore = false
        solves = emptyList()
        loadNextPage()
        isLoading = false
    }

    LaunchedEffect(username) {
        if (solves.isEmpty()) reload()
    }

    // `null` disables paging in [AllSolvesScreen]; built as a stored value rather than
    // inline at the call site so the composable body stays cheap to type-check.
    val loadMoreHandler: (suspend () -> Unit)? = if (hasMore) {
        { loadNextPage() }
    } else {
        null
    }

    val currentError = errorMessage

    when {
        isLoading && solves.isEmpty() -> LoadingState()

        currentError != null && solves.isEmpty() -> ErrorState(
            message = currentError,
            onRetry = { scope.launch { reload() } }
        )

        solves.isEmpty() -> EmptyState(username = username)

        else -> AllSolvesScreen(
            solves = solves,
            onBack = onBack,
            title = "$username's Solves",
            onLoadMore = loadMoreHandler
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ErrorView(message = message, onRetry = onRetry)
    }
}

@Composable
private fun EmptyState(username: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyStateView(
            icon = Icons.Default.Square,
            title = "No Solves Yet",
            message = "@$username has not logged a solve yet."
        )
    }
}
