package com.traverse.android.ui.friends

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.traverse.android.data.Friend
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.getLeaderboard
import com.traverse.android.viewmodel.isSelf
import com.traverse.android.viewmodel.getTotalPendingCount
import com.traverse.android.viewmodel.getFriendStreakCount

// Pastel colors matching Android app's monochromish-pastel theme
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

// Leaderboard colors
private val GoldColor = Color(0xFFFFD700)
private val SilverColor = Color(0xFFC0C0CC)
private val BronzeColor = Color(0xFFCD7F32)

object FriendsDestinations {
    const val FRIENDS_MAIN = "friends_main"
    const val USER_PROFILE = "user_profile/{username}"

    fun userProfile(username: String) = "user_profile/$username"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FriendsDestinations.FRIENDS_MAIN,
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
    ) {
        composable(FriendsDestinations.FRIENDS_MAIN) {
            FriendsMainContent(
                uiState = uiState,
                onRefresh = { viewModel.refresh() },
                onNavigateToProfile = { username ->
                    navController.navigate(FriendsDestinations.userProfile(username))
                },
                onShowRequestsSheet = { viewModel.showRequestsSheet() },
                onShowSearchSheet = { viewModel.showSearchSheet() },
                modifier = modifier
            )
        }

        composable(
            route = FriendsDestinations.USER_PROFILE,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            UserProfileScreen(
                username = username,
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Friend Requests Bottom Sheet
    if (uiState.showRequestsSheet) {
        FriendRequestsSheet(
            receivedRequests = uiState.receivedRequests,
            sentRequests = uiState.sentRequests,
            receivedStreakRequests = uiState.receivedStreakRequests,
            sentStreakRequests = uiState.sentStreakRequests,
            processingId = uiState.processingRequestId,
            onAccept = { viewModel.acceptRequest(it) },
            onReject = { viewModel.rejectRequest(it) },
            onCancel = { viewModel.cancelRequest(it) },
            onAcceptStreak = { viewModel.acceptStreakRequest(it) },
            onRejectStreak = { viewModel.rejectStreakRequest(it) },
            onCancelStreak = { viewModel.cancelStreakRequest(it) },
            onDismiss = { viewModel.hideRequestsSheet() }
        )
    }

    // User Search Bottom Sheet
    if (uiState.showSearchSheet) {
        UserSearchSheet(
            onDismiss = { viewModel.hideSearchSheet() },
            onNavigateToProfile = { username ->
                viewModel.hideSearchSheet()
                navController.navigate(FriendsDestinations.userProfile(username))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsMainContent(
    uiState: com.traverse.android.viewmodel.FriendsUiState,
    onRefresh: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onShowRequestsSheet: () -> Unit,
    onShowSearchSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leaderboard = uiState.getLeaderboard()
    val receivedCount = uiState.getTotalPendingCount()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                actions = {
                    // Search button
                    IconButton(onClick = onShowSearchSheet) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = "Add Friend"
                        )
                    }

                    // Requests button with badge
                    BadgedBox(
                        badge = {
                            if (receivedCount > 0) {
                                Badge { Text("$receivedCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = onShowRequestsSheet) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = "Friend Requests"
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        PullToRefreshBox(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            onRefresh = onRefresh,
            isRefreshing = uiState.isLoading
        ) {
            when {
                uiState.isLoading && uiState.friends.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && uiState.friends.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefresh) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.friends.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                    ) {
                        item {
                            EmptyFriendsContent(onAddFriend = onShowSearchSheet)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                    ) {
                        // Leaderboard Section
                        if (leaderboard.isNotEmpty()) {
                            item {
                                LeaderboardCard(
                                    leaderboard = leaderboard,
                                    uiState = uiState,
                                    onFriendClick = onNavigateToProfile
                                )
                            }
                        }

                        // All Friends Section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Friends",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = BelfastGroteskBlackFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentPastel.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${uiState.friends.size}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPastel
                                        )
                                    )
                                }
                            }
                        }

                        // Friends List
                        items(
                            items = uiState.friends,
                            key = { it.id }
                        ) { friend ->
                            FriendRow(
                                friend = friend,
                                streakCount = uiState.getFriendStreakCount(friend.username),
                                onClick = { onNavigateToProfile(friend.username) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCard(
    leaderboard: List<Friend>,
    uiState: com.traverse.android.viewmodel.FriendsUiState,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Belfast typography
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Top performers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Leaderboard entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                leaderboard.forEachIndexed { index, friend ->
                    val isSelf = uiState.isSelf(friend)
                    LeaderboardRow(
                        rank = index + 1,
                        friend = friend,
                        isSelf = isSelf,
                        onClick = { if (!isSelf) onFriendClick(friend.username) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    friend: Friend,
    isSelf: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (rankColor, bgColor) = when (rank) {
        1 -> GoldColor to GoldColor.copy(alpha = 0.12f)
        2 -> SilverColor to SilverColor.copy(alpha = 0.10f)
        3 -> BronzeColor to BronzeColor.copy(alpha = 0.10f)
        else -> Color.Gray to Color.Gray.copy(alpha = 0.08f)
    }

    val effectiveBgColor = if (isSelf) AccentPastel.copy(alpha = 0.15f) else bgColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(effectiveBgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Rank number with circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = rankColor
                )
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            rankColor.copy(alpha = 0.6f),
                            rankColor.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = friend.username.first().uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }

        // Username and stats
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = friend.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BelfastGroteskBlackFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelf) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentPastel.copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = BelfastGroteskBlackFamily,
                                fontWeight = FontWeight.Bold,
                                color = AccentPastel
                            )
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // XP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${friend.totalXp}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        // Streak with fire icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = HardPastel,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "${friend.currentStreak}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = rankColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendRow(
    friend: Friend,
    streakCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(end = 40.dp), // Space for streak number
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentPastel, EasyPastel)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.username.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = friend.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BelfastGroteskBlackFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Streak
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = HardPastel,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${friend.currentStreak}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }

                    // XP
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${friend.totalXp}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
            
            // Large streak number overlay (like iOS)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (streakCount != null) {
                    Text(
                        text = streakCount.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 64.sp,
                            color = HardPastel.copy(alpha = 0.25f)
                        )
                    )
                } else {
                    Text(
                        text = "Start!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = HardPastel.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsContent(
    onAddFriend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentPastel.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PeopleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = AccentPastel.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "No Friends Yet",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            Text(
                text = "Search for users and send friend requests to start building your network",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddFriend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPastel
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find Friends",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

