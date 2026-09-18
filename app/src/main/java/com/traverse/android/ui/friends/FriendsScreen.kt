package com.traverse.android.ui.friends

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.paletteColorAt
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.getFriendStreakCount
import com.traverse.android.viewmodel.getLeaderboard
import com.traverse.android.viewmodel.getTotalPendingCount

// Pastel colors matching Android app's monochromish-pastel theme
private val CardBackground = Color(0xFF1A1A1A)

// iOS rank colours: gold (1.0, 0.84, 0), silver (0.75, 0.75, 0.8), bronze (0.8, 0.5, 0.2).
private val GoldColor = Color(0xFFFFD600)
private val SilverColor = Color(0xFFBFBFCC)
private val BronzeColor = Color(0xFFCC8033)

object FriendsDestinations {
    const val FRIENDS_MAIN = "friends_main"
    const val USER_PROFILE = "user_profile/{username}"
    const val USER_PROFILE_SOLVES = "user_profile_solves/{username}"
    const val USER_PROFILE_AWARDS = "user_profile_awards/{username}/{isFriend}"

    fun userProfile(username: String) = "user_profile/$username"
    fun userProfileSolves(username: String) = "user_profile_solves/$username"
    fun userProfileAwards(username: String, isFriend: Boolean) =
        "user_profile_awards/$username/$isFriend"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var showQRCode by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }

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
                onShowQRCode = { showQRCode = true },
                onShowQRScanner = { showQRScanner = true },
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
                onBack = { navController.popBackStack() },
                onShowSolves = { name ->
                    navController.navigate(FriendsDestinations.userProfileSolves(name))
                },
                onShowAwards = { name, isFriend ->
                    navController.navigate(FriendsDestinations.userProfileAwards(name, isFriend))
                }
            )
        }

        // iOS: profileActivityLinks > Solves row -> ProfileSolvesView(username:isFriend:)
        composable(
            route = FriendsDestinations.USER_PROFILE_SOLVES,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            ProfileSolvesScreen(
                username = username,
                onBack = { navController.popBackStack() }
            )
        }

        // iOS: profileActivityLinks > Awards row -> AllAchievementsView(source:)
        composable(
            route = FriendsDestinations.USER_PROFILE_AWARDS,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("isFriend") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            val isFriend = backStackEntry.arguments?.getBoolean("isFriend") ?: false
            ProfileAwardsScreen(
                username = username,
                isFriend = isFriend,
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
    
    // QR Code Sheet
    if (showQRCode) {
        uiState.currentUser?.let { user ->
            QRCodeSheet(
                username = user.username,
                onDismiss = { showQRCode = false }
            )
        }
    }
    
    // QR Scanner
    if (showQRScanner) {
        QRScannerScreen(
            currentUsername = uiState.currentUser?.username ?: "",
            onBack = { showQRScanner = false },
            onUserScanned = { username ->
                showQRScanner = false
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
    onShowQRCode: () -> Unit,
    onShowQRScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leaderboard = uiState.getLeaderboard()
    val receivedCount = uiState.getTotalPendingCount()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    // QR Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Show My QR Code") },
                                onClick = {
                                    showMenu = false
                                    onShowQRCode()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan QR Code") },
                                onClick = {
                                    showMenu = false
                                    onShowQRScanner()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                    
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
                        modifier = Modifier.fillMaxSize(),
                        // Clears the floating bottom bar; the list still scrolls underneath it.
                        contentPadding = PaddingValues(
                            bottom = floatingBottomBarContentInset()
                        )
                    ) {
                        item {
                            EmptyFriendsContent(modifier = Modifier.fillParentMaxSize())
                        }
                    }
                }

                else -> {
                    // iOS `friendsList`: a ScrollView whose VStack (spacing 20) holds the leaderboard,
                    // the "ALL FRIENDS" header and the rows, with the rows sitting on a gray6 surface
                    // rounded to 12. The rows share that surface, so the 16pt gaps between them read
                    // as part of the same block.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        // Clears the floating bottom bar; the list still scrolls underneath it.
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = floatingBottomBarContentInset()
                        )
                    ) {
                        // Leaderboard Section
                        if (leaderboard.isNotEmpty()) {
                            item {
                                LeaderboardSection(leaderboard = leaderboard.take(3))
                            }
                        }

                        // All Friends Section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ALL FRIENDS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = "${uiState.friends.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = paletteColorAt(3)
                                    )
                                )
                            }
                        }

                        // Friends List
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackground),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                uiState.friends.forEach { friend ->
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
    }
}

/**
 * iOS `LeaderboardSection`: one card washed with a top-leading to bottom-trailing gradient of the
 * first three palette colours, a trophy header, and the top three ranked names.
 */
@Composable
private fun LeaderboardSection(
    leaderboard: List<Friend>,
    modifier: Modifier = Modifier
) {
    if (leaderboard.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PeopleOutline,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add friends to see the leaderboard",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        paletteColorAt(0).copy(alpha = 0.4f),
                        paletteColorAt(1).copy(alpha = 0.3f),
                        paletteColorAt(2).copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: yellow trophy plus an uppercase caption.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LEADERBOARD",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            // Divider after the header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // Top 3 names, separated by hairlines inset past the rank glyph
            Column {
                leaderboard.take(3).forEachIndexed { index, friend ->
                    LeaderboardNameRow(rank = index + 1, friend = friend)

                    if (index < minOf(leaderboard.size, 3) - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS `LeaderboardNameRow`: rank glyph, name and XP, with the type scale and the row opacity
 * stepping down from first to third place. iOS renders these as plain rows, so they are not
 * tappable.
 */
@Composable
private fun LeaderboardNameRow(
    rank: Int,
    friend: Friend,
    modifier: Modifier = Modifier
) {
    val rankColor = when (rank) {
        1 -> GoldColor
        2 -> SilverColor
        else -> BronzeColor
    }

    // iOS uses `crown.fill` for first place and `medal.fill` for second and third. The app already
    // maps those two symbols to WorkspacePremium and Verified in AllAchievementsScreen.
    val rankIcon = if (rank == 1) Icons.Default.WorkspacePremium else Icons.Default.Verified
    val iconSize = if (rank == 1) 20.dp else 18.dp
    val nameSize = if (rank == 1) 22.sp else 18.sp
    val xpSize = if (rank == 1) 20.sp else 16.sp
    val rowOpacity = when (rank) {
        1 -> 1f
        2 -> 0.9f
        else -> 0.8f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .alpha(rowOpacity),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank glyph, inside a fixed 32dp box so the names line up across ranks
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = rankIcon,
                contentDescription = null,
                tint = rankColor,
                modifier = Modifier.size(iconSize)
            )
        }

        // The name absorbs the slack so the XP block sits on the trailing edge, like iOS's Spacer
        Text(
            text = friend.username,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = nameSize,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // XP, baseline-aligned with its unit label
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${friend.totalXp}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = xpSize,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                ),
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "XP",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    streakCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // iOS shows the glow whenever a friend streak exists, even at 0 days.
    val hasActiveStreak = streakCount != null
    val streakColor = paletteColorAt(0)

    // iOS breathes this glow with `.repeatForever`. As with the home achievement card, the value is
    // pinned to the mid-point of the iOS range (its `sin(glowPhase)` factor averages 0.5) so the
    // resting appearance matches without a per-frame recomposition of the whole list.
    val glowFillOpacity = if (hasActiveStreak) 0.15f + 0.1f * 0.5f else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            // iOS: `.background(systemGray6)` then `.clipShape(RoundedRectangle(cornerRadius: 12))`
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // iOS: `.padding(16)` then `.padding(.trailing, 40)` to leave room for the number
                .padding(16.dp)
                .padding(end = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar, filled with the third palette colour at 50x50
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                paletteColorAt(2),
                                paletteColorAt(2).copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = paletteColorAt(0),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${friend.currentStreak}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = paletteColorAt(1),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${friend.totalXp}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Oversized streak number, or "Start!" when there is no streak yet. iOS pins this to the
        // trailing edge and nudges it with an offset in both cases.
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
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = streakColor.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.offset(x = 10.dp)
                )
            } else {
                Text(
                    text = "Start!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = streakColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.offset(x = (-8).dp)
                )
            }
        }

        // Bottom gradient wash while a streak is active. iOS overlays a vertical gradient that stays
        // clear for two thirds and fades into the streak colour at the bottom edge.
        if (hasActiveStreak) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                streakColor.copy(alpha = glowFillOpacity)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun EmptyFriendsContent(
    modifier: Modifier = Modifier
) {
    // iOS `EmptyFriendsView`: a large `person.2.slash` over a bold headline and a centred subtitle.
    // iOS has no call to action here, search lives in the toolbar.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyStateView(
            icon = Icons.Default.PeopleOutline,
            title = "No Friends Yet",
            message = "Search for someone by username, or scan their QR code, to start " +
                "comparing streaks and progress."
        )
    }
}

