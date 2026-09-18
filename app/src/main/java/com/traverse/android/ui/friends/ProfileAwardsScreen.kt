package com.traverse.android.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.traverse.android.data.AllAchievementsResponse
import com.traverse.android.data.AwardSection
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.ui.home.AllAchievementsScreen
import com.traverse.android.ui.home.AwardsSectionScreen
import com.traverse.android.ui.home.fallbackSections
import kotlinx.coroutines.launch

/**
 * Someone else's award shelf, rendered with the same [AllAchievementsScreen] the
 * home feed uses — mirroring iOS, where the profile pushes `AllAchievementsView(source:)`
 * so there is one award view for your shelf and a friend's. Friends go through the
 * friends route (which additionally applies the block rules); everyone else through
 * the public per-user route, gated by their visibility setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAwardsScreen(
    username: String,
    isFriend: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    // `load()` is a suspend function, so a retry tap has to hand it to a coroutine.
    val scope = rememberCoroutineScope()

    var response by remember { mutableStateOf<AllAchievementsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<AwardSection?>(null) }

    suspend fun load() {
        isLoading = true
        val result = if (isFriend) {
            networkService.getFriendAchievements(username)
        } else {
            networkService.getUserAchievements(username)
        }
        when (result) {
            is NetworkResult.Success -> {
                response = result.data
                errorMessage = null
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }

    LaunchedEffect(username, isFriend) {
        load()
    }

    Box(modifier = Modifier) {
        AllAchievementsScreen(
            achievements = response?.achievements ?: emptyList(),
            stats = null,
            sections = response?.sections ?: emptyList(),
            featured = response?.featured,
            title = "$username's Awards",
            isLoading = isLoading,
            errorMessage = errorMessage,
            emptyDescription = "$username has not earned an award yet.",
            onBack = onBack,
            onRetry = { scope.launch { load() } },
            onOpenSection = { sectionId ->
                val shelves = response?.sections?.ifEmpty {
                    response?.achievements?.let { fallbackSections(it) } ?: emptyList()
                } ?: emptyList()
                selectedSection = shelves.find { it.id == sectionId }
            }
        )

        // One shelf, opened. iOS pushes `AwardsSectionView(section:)`; the overlay
        // keeps the award hub's state (and scroll) alive behind it.
        selectedSection?.let { section ->
            Box(modifier = Modifier.align(Alignment.Center)) {
                AwardsSectionScreen(
                    section = section,
                    onBack = { selectedSection = null }
                )
            }
        }
    }
}
