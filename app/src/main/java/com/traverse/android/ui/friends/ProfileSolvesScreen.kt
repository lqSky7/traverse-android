package com.traverse.android.ui.friends

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Solve
import com.traverse.android.ui.home.AllSolvesScreen

/**
 * A friend's solve history, rendered with the same [AllSolvesScreen] the home feed uses.
 * Cursor-paginated so long histories load smoothly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSolvesScreen(
    username: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }

    var solves by remember { mutableStateOf<List<Solve>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var nextCursor by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadPage(cursor: Int? = null, append: Boolean = false) {
        if (!append) isLoading = true
        when (val result = networkService.getFriendSolves(username, limit = 50, cursor = cursor)) {
            is NetworkResult.Success -> {
                val data = result.data
                solves = if (append) solves + data.solves else data.solves
                nextCursor = data.pagination.nextCursor
                errorMessage = null
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }

    LaunchedEffect(username) {
        loadPage()
    }

    AllSolvesScreen(
        solves = solves,
        title = "$username's Solves",
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onLoadMore = nextCursor?.let { cursor ->
            { loadPage(cursor = cursor, append = true) }
        },
        onRetry = { loadPage() }
    )
}
