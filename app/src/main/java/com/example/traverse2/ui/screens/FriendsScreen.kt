package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traverse2.data.api.FriendItem
import com.example.traverse2.data.api.FriendStreakItem
import com.example.traverse2.data.api.ReceivedFriendRequest
import com.example.traverse2.data.api.ReceivedStreakRequest
import com.example.traverse2.data.api.SentFriendRequest
import com.example.traverse2.data.api.SentStreakRequest
import com.example.traverse2.data.api.UserSearchResult
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import com.example.traverse2.ui.viewmodel.FriendsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

@Composable
fun FriendsScreen(
    hazeState: HazeState,
    onFriendClick: (FriendItem) -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val glassColors = TraverseTheme.glassColors
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Friends", "Requests", "Streaks")

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Request badge
                val totalRequests = uiState.receivedRequests.size + uiState.receivedStreakRequests.size
                if (totalRequests > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalRequests pending",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Add friend button
                IconButton(
                    onClick = { showAddFriendDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x20000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Friend",
                        tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        GlassTabRow(
            selectedTabIndex = selectedTab,
            tabs = tabs,
            glassColors = glassColors,
            hazeState = hazeState,
            onTabSelected = { selectedTab = it },
            badgeCounts = listOf(
                uiState.friends.size,
                uiState.receivedRequests.size + uiState.sentRequests.size,
                uiState.friendStreaks.size
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
                )
            }
        } else {
            when (selectedTab) {
                0 -> FriendsListTab(
                    friends = uiState.friends,
                    hazeState = hazeState,
                    glassColors = glassColors,
                    onFriendClick = onFriendClick,
                    onRemoveFriend = { viewModel.removeFriend(it.username) },
                    onStartStreak = { viewModel.sendStreakRequest(it.username) },
                    isVisible = isVisible
                )
                1 -> RequestsTab(
                    receivedRequests = uiState.receivedRequests,
                    sentRequests = uiState.sentRequests,
                    hazeState = hazeState,
                    glassColors = glassColors,
                    onAccept = { viewModel.acceptFriendRequest(it) },
                    onReject = { viewModel.rejectFriendRequest(it) },
                    onCancel = { viewModel.cancelFriendRequest(it) },
                    isVisible = isVisible
                )
                2 -> StreaksTab(
                    friendStreaks = uiState.friendStreaks,
                    receivedRequests = uiState.receivedStreakRequests,
                    sentRequests = uiState.sentStreakRequests,
                    hazeState = hazeState,
                    glassColors = glassColors,
                    onAccept = { viewModel.acceptStreakRequest(it) },
                    onReject = { viewModel.rejectStreakRequest(it) },
                    onCancel = { viewModel.cancelStreakRequest(it) },
                    onDeleteStreak = { viewModel.deleteFriendStreak(it) },
                    isVisible = isVisible
                )
            }
        }
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        AddFriendDialog(
            hazeState = hazeState,
            glassColors = glassColors,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            friends = uiState.friends,
            sentRequests = uiState.sentRequests,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSendRequest = { viewModel.sendFriendRequest(it) },
            onDismiss = {
                showAddFriendDialog = false
                viewModel.clearSearch()
            }
        )
    }
}

@Composable
private fun GlassTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    glassColors: GlassColors,
    hazeState: HazeState,
    onTabSelected: (Int) -> Unit,
    badgeCounts: List<Int>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
                    blurRadius = 20.dp,
                    tint = HazeTint(if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = glassColors.textPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (badgeCounts[index] > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedTabIndex == index)
                                                (if (glassColors.isDark) Color.White else Color(0xFFE91E8C))
                                            else
                                                glassColors.textSecondary.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${badgeCounts[index]}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTabIndex == index)
                                            (if (glassColors.isDark) Color.Black else Color.White)
                                        else
                                            glassColors.textPrimary
                                    )
                                }
                            }
                        }
                    },
                    selectedContentColor = glassColors.textPrimary,
                    unselectedContentColor = glassColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun FriendsListTab(
    friends: List<FriendItem>,
    hazeState: HazeState,
    glassColors: GlassColors,
    onFriendClick: (FriendItem) -> Unit,
    onRemoveFriend: (FriendItem) -> Unit,
    onStartStreak: (FriendItem) -> Unit,
    isVisible: Boolean
) {
    if (friends.isEmpty()) {
        EmptyState(
            icon = Icons.Default.People,
            title = "No friends yet",
            subtitle = "Add friends to see them here",
            glassColors = glassColors
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(friends) { index, friend ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { 100 }
                    )
                ) {
                    var showCard by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 60L)
                        showCard = true
                    }

                    if (showCard) {
                        FriendCard(
                            friend = friend,
                            hazeState = hazeState,
                            glassColors = glassColors,
                            onClick = { onFriendClick(friend) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendItem,
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)

    // Generate a color based on username hash
    val profileColor = remember(friend.username) {
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
        )
        colors[friend.username.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = 24.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                profileColor,
                                profileColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = profileColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.first().uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Streak
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${friend.currentStreak}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textSecondary
                        )
                    }

                    // XP
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "XP",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${friend.totalXp}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textSecondary
                        )
                    }
                }
            }

            // Arrow/chevron indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x10000000)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "View profile",
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RequestsTab(
    receivedRequests: List<ReceivedFriendRequest>,
    sentRequests: List<SentFriendRequest>,
    hazeState: HazeState,
    glassColors: GlassColors,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    isVisible: Boolean
) {
    if (receivedRequests.isEmpty() && sentRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.PersonAdd,
            title = "No pending requests",
            subtitle = "Friend requests will appear here",
            glassColors = glassColors
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (receivedRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Received",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(receivedRequests) { index, request ->
                    RequestCard(
                        username = request.requester.username,
                        streak = request.requester.currentStreak,
                        xp = request.requester.totalXp,
                        isReceived = true,
                        hazeState = hazeState,
                        glassColors = glassColors,
                        onAccept = { onAccept(request.id) },
                        onReject = { onReject(request.id) },
                        onCancel = {}
                    )
                }
            }

            if (sentRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Sent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(sentRequests) { index, request ->
                    RequestCard(
                        username = request.addressee.username,
                        streak = request.addressee.currentStreak,
                        xp = request.addressee.totalXp,
                        isReceived = false,
                        hazeState = hazeState,
                        glassColors = glassColors,
                        onAccept = {},
                        onReject = {},
                        onCancel = { onCancel(request.id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun RequestCard(
    username: String,
    streak: Int,
    xp: Int,
    isReceived: Boolean,
    hazeState: HazeState,
    glassColors: GlassColors,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)

    val profileColor = remember(username) {
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
        )
        colors[username.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = 24.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(profileColor, profileColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.first().uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$streak streak",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                    Text(
                        text = "$xp XP",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                }
            }

            // Action buttons
            if (isReceived) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreaksTab(
    friendStreaks: List<FriendStreakItem>,
    receivedRequests: List<ReceivedStreakRequest>,
    sentRequests: List<SentStreakRequest>,
    hazeState: HazeState,
    glassColors: GlassColors,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onDeleteStreak: (String) -> Unit,
    isVisible: Boolean
) {
    if (friendStreaks.isEmpty() && receivedRequests.isEmpty() && sentRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Whatshot,
            title = "No friend streaks",
            subtitle = "Start a streak with a friend to track your progress together",
            glassColors = glassColors
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (receivedRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Streak Requests",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(receivedRequests) { _, request ->
                    RequestCard(
                        username = request.requester.username,
                        streak = request.requester.currentStreak,
                        xp = request.requester.totalXp,
                        isReceived = true,
                        hazeState = hazeState,
                        glassColors = glassColors,
                        onAccept = { onAccept(request.id) },
                        onReject = { onReject(request.id) },
                        onCancel = {}
                    )
                }
            }

            if (sentRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending Streak Requests",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(sentRequests) { _, request ->
                    RequestCard(
                        username = request.addressee.username,
                        streak = request.addressee.currentStreak,
                        xp = request.addressee.totalXp,
                        isReceived = false,
                        hazeState = hazeState,
                        glassColors = glassColors,
                        onAccept = {},
                        onReject = {},
                        onCancel = { onCancel(request.id) }
                    )
                }
            }

            if (friendStreaks.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Streaks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(friendStreaks) { _, streak ->
                    FriendStreakCard(
                        streak = streak,
                        hazeState = hazeState,
                        glassColors = glassColors
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun FriendStreakCard(
    streak: FriendStreakItem,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)

    val profileColor = remember(streak.friend.username) {
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
        )
        colors[streak.friend.username.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = 24.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(profileColor, profileColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streak.friend.username.first().uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = streak.friend.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
                Text(
                    text = "Best: ${streak.longestStreak} days",
                    fontSize = 12.sp,
                    color = glassColors.textSecondary
                )
            }

            // Streak count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF6B35).copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${streak.currentStreak}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
            }
        }
    }
}

@Composable
private fun AddFriendDialog(
    hazeState: HazeState,
    glassColors: GlassColors,
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    friends: List<FriendItem>,
    sentRequests: List<SentFriendRequest>,
    onQueryChange: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val friendUsernames = remember(friends) { friends.map { it.username }.toSet() }
    val pendingUsernames = remember(sentRequests) { sentRequests.map { it.addressee.username }.toSet() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
                        blurRadius = 24.dp,
                        tint = HazeTint(if (glassColors.isDark) Color(0x40000000) else Color(0x40FFFFFF)),
                        noiseFactor = 0.02f
                    )
                )
                .background(if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x60FFFFFF))
                .clickable(onClick = {}) // Prevent click through
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Add Friend",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x20000000))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = glassColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = glassColors.textPrimary
                            ),
                            cursorBrush = SolidColor(if (glassColors.isDark) Color.White else Color(0xFFE91E8C)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by username...",
                                        fontSize = 16.sp,
                                        color = glassColors.textSecondary
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results
                if (searchResults.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        searchResults.forEach { user ->
                            val isFriend = user.username in friendUsernames
                            val isPending = user.username in pendingUsernames

                            SearchResultItem(
                                user = user,
                                isFriend = isFriend,
                                isPending = isPending,
                                glassColors = glassColors,
                                onAdd = { onSendRequest(user.username) }
                            )
                        }
                    }
                } else if (searchQuery.length >= 2 && !isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found",
                            color = glassColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    user: UserSearchResult,
    isFriend: Boolean,
    isPending: Boolean,
    glassColors: GlassColors,
    onAdd: () -> Unit
) {
    val profileColor = remember(user.username) {
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
        )
        colors[user.username.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x15000000))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(profileColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.first().uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary
            )
            Text(
                text = "${user.currentStreak} streak • ${user.totalXp} XP",
                fontSize = 12.sp,
                color = glassColors.textSecondary
            )
        }

        if (isFriend) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Friend",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF22C55E)
                )
            }
        } else if (isPending) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFBBF24).copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Pending",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFBBF24)
                )
            }
        } else {
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (glassColors.isDark) Color.White.copy(alpha = 0.1f)
                        else Color(0xFFE91E8C).copy(alpha = 0.1f)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    glassColors: GlassColors
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glassColors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = glassColors.textSecondary
            )
        }
    }
}
