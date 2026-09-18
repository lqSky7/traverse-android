package com.traverse.android.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traverse.android.data.CacheManager
import com.traverse.android.data.FriendStreak
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.RelationshipState
import com.traverse.android.data.SendFriendStreakRequestBody
import com.traverse.android.data.UserProfile
import com.traverse.android.data.UserStatisticsData
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.paletteColorAt
import com.traverse.android.ui.theme.palettePrimary
import kotlinx.coroutines.launch

// Pastel colors
private val CardBackground = Color(0xFF1A1A1A)

enum class FriendshipStatus {
    CURRENT_USER,
    NOT_FRIENDS,
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    BLOCKED
}

enum class FriendStreakStatus {
    NONE,
    ACTIVE,
    CAN_START,
    REQUEST_SENT,
    REQUEST_RECEIVED
}

/**
 * Someone's profile, rebuilt to match iOS `UserProfileView`.
 *
 * The page is deliberately shallow: an identity header, whatever action the
 * relationship allows, the statistics, and two links into the *shared* solve
 * list and Awards hub. The previous version was an inline tab picker with its
 * own solve list and badge grid, and inferred the relationship from eight
 * parallel network calls instead of asking the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    onBack: () -> Unit,
    onShowSolves: (String) -> Unit = {},
    onShowAwards: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    val cacheManager = remember { CacheManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    // State
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var statistics by remember { mutableStateOf<UserStatisticsData?>(null) }
    var relationship by remember { mutableStateOf<RelationshipState?>(null) }
    var friendshipStatus by remember { mutableStateOf(FriendshipStatus.NOT_FRIENDS) }
    var isActionLoading by remember { mutableStateOf(false) }

    // Friend Streak State
    var friendStreakStatus by remember { mutableStateOf(FriendStreakStatus.NONE) }
    var activeFriendStreak by remember { mutableStateOf<FriendStreak?>(null) }

    // Gift freeze state
    var isGiftingFreeze by remember { mutableStateOf(false) }
    var giftSuccessMessage by remember { mutableStateOf<String?>(null) }

    val isFriend = friendshipStatus == FriendshipStatus.FRIENDS

    /**
     * Maps the server's relationship status onto the view's presentation state.
     * The streak sub-state comes from the same payload, so a 0-day streak is no
     * longer rendered as an active one (iOS `applyRelationship`).
     */
    fun applyRelationship(state: RelationshipState) {
        relationship = state
        friendshipStatus = when (state.status) {
            "self" -> FriendshipStatus.CURRENT_USER
            "friends" -> FriendshipStatus.FRIENDS
            "pending_outgoing" -> FriendshipStatus.REQUEST_SENT
            "pending_incoming" -> FriendshipStatus.REQUEST_RECEIVED
            "blocked" -> FriendshipStatus.BLOCKED
            else -> FriendshipStatus.NOT_FRIENDS
        }

        val streak = state.streak
        if (streak == null) {
            friendStreakStatus = FriendStreakStatus.NONE
            activeFriendStreak = null
        } else {
            friendStreakStatus =
                if (streak.currentStreak > 0) FriendStreakStatus.ACTIVE else FriendStreakStatus.CAN_START
        }
    }

    /** Re-reads the relationship from the server after any action that could change it. */
    suspend fun refreshRelationship() {
        when (val result = networkService.getRelationship(username)) {
            is NetworkResult.Success -> applyRelationship(result.data)
            is NetworkResult.Error -> Unit // the action itself already reported its outcome
        }
    }

    /**
     * iOS `loadFriendStreakStatus`: the relationship payload narrows the state to
     * active/can-start, but the active-streak *detail* (best-streak count) and the
     * pending streak requests still need their own fetches.
     */
    suspend fun loadStreakStatus() {
        if (friendshipStatus != FriendshipStatus.FRIENDS) {
            friendStreakStatus = FriendStreakStatus.NONE
            activeFriendStreak = null
            return
        }

        // Check for active streaks
        when (val streaksResult = networkService.getFriendStreaks()) {
            is NetworkResult.Success -> {
                streaksResult.data.streaks.firstOrNull { it.friend.username == username }
                    ?.let { existing ->
                        activeFriendStreak = existing
                        friendStreakStatus = FriendStreakStatus.ACTIVE
                        return
                    }
                // fall through to pending-request checks
            }
            is NetworkResult.Error -> Unit // silently fail - not critical
        }

        val sent = (networkService.getSentFriendStreakRequests() as? NetworkResult.Success)
            ?.data?.requests.orEmpty()
        val received = (networkService.getReceivedFriendStreakRequests() as? NetworkResult.Success)
            ?.data?.requests.orEmpty()

        friendStreakStatus = when {
            sent.any { it.requested?.username == username } -> FriendStreakStatus.REQUEST_SENT
            received.any { it.requester?.username == username } -> FriendStreakStatus.REQUEST_RECEIVED
            else -> FriendStreakStatus.CAN_START
        }
    }

    suspend fun loadProfile(force: Boolean) {
        if (!force) isLoading = true
        errorMessage = null

        // Try cache first for profile (first load only)
        if (!force) {
            cacheManager.getUserProfile(username)?.let { profile = it.user }
        }

        // One round trip answers profile, stats and the relationship question. The
        // previous version fetched the friends list plus both request lists and both
        // streak request lists to render a single button.
        val profileResult = networkService.getUserProfile(username)
        val statsResult = networkService.getUserStatistics(username)
        val relationshipResult = networkService.getRelationship(username)

        when (profileResult) {
            is NetworkResult.Success -> {
                profile = profileResult.data.user
                cacheManager.cacheUserProfile(username, profileResult.data)
            }
            is NetworkResult.Error -> {
                errorMessage = profileResult.message
            }
        }

        when (statsResult) {
            is NetworkResult.Success -> statistics = statsResult.data.stats
            is NetworkResult.Error -> Unit // Silently fail for stats
        }

        when (relationshipResult) {
            is NetworkResult.Success -> applyRelationship(relationshipResult.data)
            is NetworkResult.Error -> {
                if (profile == null) errorMessage = relationshipResult.message
            }
        }

        loadStreakStatus()
        isLoading = false
    }

    LaunchedEffect(username) {
        loadProfile(force = false)
    }

    // Actions. Every one that could change the relationship re-reads the
    // authoritative state from the server rather than guessing the outcome.
    fun sendFriendRequest() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.sendFriendRequest(username)) {
                // The server may have sent a request, returned an idempotent "already
                // requested", or auto-accepted because they had already asked us.
                is NetworkResult.Success -> refreshRelationship()
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun removeFriend() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.removeFriend(username)) {
                is NetworkResult.Success -> refreshRelationship()
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun blockUser() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.blockUser(username)) {
                is NetworkResult.Success -> refreshRelationship()
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun unblockUser() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.unblockUser(username)) {
                is NetworkResult.Success -> refreshRelationship()
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun toggleFavorite() {
        val state = relationship ?: return
        if (!state.isFriends) return
        scope.launch {
            isActionLoading = true
            val next = !(state.friendship?.favorite ?: false)
            when (val result = networkService.setFriendFavorite(username, next)) {
                is NetworkResult.Success -> refreshRelationship()
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun sendStreakRequest() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.sendFriendStreakRequest(
                SendFriendStreakRequestBody(username)
            )) {
                is NetworkResult.Success -> friendStreakStatus = FriendStreakStatus.REQUEST_SENT
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun deleteStreak() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.deleteFriendStreak(username)) {
                is NetworkResult.Success -> {
                    friendStreakStatus = FriendStreakStatus.CAN_START
                    activeFriendStreak = null
                }
                is NetworkResult.Error -> errorMessage = result.message
            }
            isActionLoading = false
        }
    }

    fun giftFreeze() {
        scope.launch {
            isGiftingFreeze = true
            when (val result = networkService.giftFreezes(username, 1)) {
                is NetworkResult.Success -> giftSuccessMessage = "Freeze gifted to $username!"
                is NetworkResult.Error -> errorMessage = result.message
            }
            isGiftingFreeze = false
        }
    }

    // Auto-clear success message after 3 seconds
    LaunchedEffect(giftSuccessMessage) {
        giftSuccessMessage?.let {
            kotlinx.coroutines.delay(3000)
            giftSuccessMessage = null
        }
    }

    // Clear error messages after 5 seconds
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
    }

    // Dialog state
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showGiftDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    /**
     * Whether the top-right menu has anything in it. An empty menu still opens an
     * empty popover, so the item is omitted rather than shown blank (mirrors iOS
     * `showsActionsMenu`).
     */
    val showsActionsMenu = when (friendshipStatus) {
        FriendshipStatus.FRIENDS -> true
        // Blocking is offered wherever you can act on someone — the case you most
        // need it in is a stranger who will not stop.
        FriendshipStatus.NOT_FRIENDS,
        FriendshipStatus.REQUEST_SENT,
        FriendshipStatus.REQUEST_RECEIVED -> relationship?.canRequest == true
        // A blocked profile shows its own inline "Unblock" recovery button, and your
        // own profile has nothing to manage.
        FriendshipStatus.BLOCKED, FriendshipStatus.CURRENT_USER -> false
    }

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(username) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (showsActionsMenu) {
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
                                if (friendshipStatus == FriendshipStatus.FRIENDS) {
                                    val isFavorite = relationship?.friendship?.favorite == true
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isFavorite) "Remove from Close Friends"
                                                else "Add to Close Friends"
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            toggleFavorite()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Default.StarBorder
                                                else Icons.Default.Star,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Remove Friend") },
                                        onClick = {
                                            showMenu = false
                                            showRemoveDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PersonRemove,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Block @$username") },
                                    onClick = {
                                        showMenu = false
                                        showBlockDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadProfile(force = true)
                    isRefreshing = false
                }
            },
            isRefreshing = isRefreshing
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null && profile == null -> {
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
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                scope.launch { loadProfile(force = true) }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                profile != null -> {
                    ProfileBody(
                        profile = profile!!,
                        statistics = statistics,
                        friendshipStatus = friendshipStatus,
                        relationship = relationship,
                        friendStreakStatus = friendStreakStatus,
                        activeFriendStreak = activeFriendStreak,
                        isActionLoading = isActionLoading,
                        onSendRequest = { sendFriendRequest() },
                        onUnblock = { unblockUser() },
                        onSendStreakRequest = { sendStreakRequest() },
                        onDeleteStreak = { deleteStreak() },
                        onGift = { showGiftDialog = true },
                        onShowSolves = onShowSolves,
                        onShowAwards = onShowAwards
                    )
                }
            }
        }
    }

    // Remove Friend Confirmation Dialog — iOS: "This also ends any streak you share
    // and cancels pending requests."
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove $username?") },
            text = { Text("This also ends any streak you share and cancels pending requests.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        removeFriend()
                    }
                ) {
                    Text("Remove Friend", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Confirmation Dialog — iOS copy verbatim
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block $username?") },
            text = {
                Text(
                    "They will not be able to send you friend or streak requests, and " +
                        "neither of you will see the other. They are not told that you blocked them."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        blockUser()
                    }
                ) {
                    Text("Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Gift Freeze Confirmation Dialog
    if (showGiftDialog) {
        AlertDialog(
            onDismissRequest = { showGiftDialog = false },
            title = { Text("Gift a Streak Freeze?") },
            text = {
                Text(
                    "This will cost 70 XP from your balance. " +
                        "$username can use it to protect their streak!"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiftDialog = false
                        giftFreeze()
                    }
                ) {
                    Text("Gift for 70 XP", color = Color.Cyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiftDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success message overlay
    giftSuccessMessage?.let { message ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(paletteColorAt(1))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * The page body: an identity header, whatever action the relationship allows,
 * the statistics, and two links into the shared solve list and Awards hub.
 */
@Composable
private fun ProfileBody(
    profile: UserProfile,
    statistics: UserStatisticsData?,
    friendshipStatus: FriendshipStatus,
    relationship: RelationshipState?,
    friendStreakStatus: FriendStreakStatus,
    activeFriendStreak: FriendStreak?,
    isActionLoading: Boolean,
    onSendRequest: () -> Unit,
    onUnblock: () -> Unit,
    onSendStreakRequest: () -> Unit,
    onDeleteStreak: () -> Unit,
    onGift: () -> Unit,
    onShowSolves: (String) -> Unit,
    onShowAwards: (String, Boolean) -> Unit
) {
    val username = profile.username
    val isFriend = friendshipStatus == FriendshipStatus.FRIENDS

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Clears the floating bottom bar; the page still scrolls underneath it.
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = floatingBottomBarContentInset()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        item {
            ProfileHeader(profile = profile, statistics = statistics)
        }

        // Friend streak section (only for friends)
        if (isFriend) {
            item {
                StreakActionSection(
                    status = friendStreakStatus,
                    activeStreak = activeFriendStreak,
                    isLoading = isActionLoading,
                    onSendStreakRequest = onSendStreakRequest,
                    onDeleteStreak = onDeleteStreak
                )
            }

            // The single on-page action. Everything destructive or housekeeping —
            // remove, block, close-friend — lives in the top-right menu instead of
            // stacking up the page.
            item {
                GiftFreezeButton(
                    isGifting = isActionLoading,
                    onGift = onGift
                )
            }
        }

        // The primary action for each relationship state. The FRIENDS and
        // CURRENT_USER cases are empty on purpose: a friend's only on-page action is
        // the freeze button above, and the rest moved into the top-right menu.
        when (friendshipStatus) {
            FriendshipStatus.NOT_FRIENDS -> item {
                Button(
                    onClick = onSendRequest,
                    enabled = !isActionLoading && relationship?.canRequest != false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palettePrimary,
                        contentColor = Color.Black
                    )
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Friend Request")
                    }
                }
            }

            FriendshipStatus.REQUEST_SENT -> item {
                // iOS: HStack { clock; "Friend Request Sent" } .foregroundStyle(.secondary)
                StatusPill(
                    icon = Icons.Default.Schedule,
                    text = "Friend Request Sent",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            FriendshipStatus.REQUEST_RECEIVED -> item {
                // iOS: HStack { envelope.badge; "Friend Request Received" } tint .blue
                StatusPill(
                    icon = Icons.Default.MarkEmailUnread,
                    text = "Friend Request Received",
                    color = Color(0xFF007AFF)
                )
            }

            FriendshipStatus.BLOCKED -> item {
                BlockedStateContent(
                    username = username,
                    isLoading = isActionLoading,
                    onUnblock = onUnblock
                )
            }

            FriendshipStatus.FRIENDS, FriendshipStatus.CURRENT_USER -> Unit
        }

        // Statistics Card
        if (statistics != null) {
            item {
                StatisticsCard(statistics = statistics)
            }
        }

        // Activity links — each opens the screen the home feed already uses
        item {
            ProfileActivityLinks(
                username = username,
                isFriend = isFriend,
                onShowSolves = onShowSolves,
                onShowAwards = onShowAwards
            )
        }
    }
}

/** iOS `streakActionSection`: the friend streak in its five sub-states. */
@Composable
private fun StreakActionSection(
    status: FriendStreakStatus,
    activeStreak: FriendStreak?,
    isLoading: Boolean,
    onSendStreakRequest: () -> Unit,
    onDeleteStreak: () -> Unit
) {
    when (status) {
        FriendStreakStatus.NONE -> Unit

        FriendStreakStatus.ACTIVE -> {
            if (activeStreak != null) {
                ActiveStreakCard(
                    friendStreak = activeStreak,
                    onDelete = onDeleteStreak,
                    isLoading = isLoading
                )
            }
        }

        FriendStreakStatus.CAN_START -> {
            Button(
                onClick = onSendStreakRequest,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = paletteColorAt(0),
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Streak")
            }
        }

        FriendStreakStatus.REQUEST_SENT -> {
            // iOS: flame.badge.checkmark tinted with colour(at: 0)
            StatusPill(
                icon = Icons.Default.LocalFireDepartment,
                text = "Streak Request Sent",
                color = paletteColorAt(0)
            )
        }

        FriendStreakStatus.REQUEST_RECEIVED -> {
            StatusPill(
                icon = Icons.Default.LocalFireDepartment,
                text = "Streak Request Received",
                color = paletteColorAt(0)
            )
        }
    }
}

/**
 * iOS `giftFreezeButton` — deliberately compact, hugging its label instead of
 * stretching edge to edge. As a full-width pill it read as the page's primary
 * action and pushed the statistics below the fold.
 */
@Composable
private fun GiftFreezeButton(
    isGifting: Boolean,
    onGift: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF00B8D9).copy(alpha = 0.15f))
            .clickable(enabled = !isGifting, onClick = onGift)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AcUnit,
            contentDescription = null,
            tint = Color.Cyan,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isGifting) "Sending…" else "Gift Freeze",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.Cyan
            )
        )
        Text(
            text = "70 XP",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
        if (isGifting) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Color.Cyan
            )
        }
    }
}

/** iOS `.blocked` case: an inline banner plus the "Unblock" recovery button. */
@Composable
private fun BlockedStateContent(
    username: String,
    isLoading: Boolean,
    onUnblock: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "You blocked @$username",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }

        Button(
            onClick = onUnblock,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Unblock")
            }
        }
    }
}

/**
 * iOS `profileActivityLinks`: a two-row card — their solves, and their awards.
 * Each opens the view the home screen already uses, so there is one solve list
 * and one award shelf in the app and a fix in either lands here too.
 */
@Composable
private fun ProfileActivityLinks(
    username: String,
    isFriend: Boolean,
    onShowSolves: (String) -> Unit,
    onShowAwards: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
    ) {
        ActivityRow(
            icon = Icons.Default.Checklist,
            tint = paletteColorAt(0),
            title = "Solves",
            subtitle = "Recent problems solved",
            onClick = { onShowSolves(username) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(start = 58.dp),
            color = Color.White.copy(alpha = 0.1f)
        )

        ActivityRow(
            icon = Icons.Default.EmojiEvents,
            tint = paletteColorAt(1),
            title = "Awards",
            subtitle = "Badges earned",
            onClick = { onShowAwards(username, isFriend) }
        )
    }
}

@Composable
private fun ActivityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    statistics: UserStatisticsData?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palettePrimary, paletteColorAt(1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.username.first().uppercase(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = profile.username,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            // Visibility badge — iOS: Label(visibility.capitalized, globe/lock/person.2)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when (profile.visibility.lowercase()) {
                        "public" -> Icons.Default.Public
                        "private" -> Icons.Default.Lock
                        else -> Icons.Default.People
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = profile.visibility.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.75f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row — iOS ProfileHeaderView: coloured value on top, then Label(icon + text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${profile.currentStreak}",
                    label = "My Streak",
                    valueColor = paletteColorAt(0),
                    modifier = Modifier.weight(1f)
                )

                ProfileStatDivider()

                StatColumn(
                    icon = Icons.Default.Star,
                    value = "${profile.totalXp}",
                    label = "XP",
                    valueColor = paletteColorAt(1),
                    modifier = Modifier.weight(1f)
                )

                if (statistics != null) {
                    ProfileStatDivider()

                    StatColumn(
                        icon = null,
                        value = "${statistics.totalSolves}",
                        label = "Solves",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .height(30.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.3f))
    )
}

@Composable
private fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/**
 * iOS `StatisticsView`: "Statistics" headline outside, then a card holding two
 * label/value rows, a divider, "Problems by Difficulty" and three tinted badges.
 */
@Composable
private fun StatisticsCard(statistics: UserStatisticsData) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow(label = "Total Submissions", value = "${statistics.totalSubmissions}")
                StatRow(label = "Total Streak Days", value = "${statistics.totalStreakDays}")

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text(
                    text = "Problems by Difficulty",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DifficultyBadge(
                        difficulty = "Easy",
                        count = statistics.problemsByDifficulty.easy,
                        color = paletteColorAt(1),
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyBadge(
                        difficulty = "Medium",
                        count = statistics.problemsByDifficulty.medium,
                        color = paletteColorAt(2),
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyBadge(
                        difficulty = "Hard",
                        count = statistics.problemsByDifficulty.hard,
                        color = paletteColorAt(0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** iOS `StatRow`: label on the left (.secondary), bold value on the right. */
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

/** iOS `DifficultyBadge`: count on top in the badge colour, label beneath, tinted background. */
@Composable
private fun DifficultyBadge(
    difficulty: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

/**
 * iOS-style status pill: HStack { icon; text } with a 10%-opacity tinted background
 * and 12pt corner radius. Used for friend-request and streak-request states.
 */
@Composable
private fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = color)
        )
    }
}

@Composable
private fun ActiveStreakCard(
    friendStreak: FriendStreak,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // iOS ActiveStreakCard.streakColor = paletteManager.color(at: 2)
    val streakColor = paletteColorAt(2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Friend Streak column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = streakColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${friendStreak.currentStreak}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Text(
                        text = "With ${friendStreak.friend.username}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                // Best streak column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${friendStreak.longestStreak}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Best",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // Android has no context menu idiom, so keep an explicit End Streak affordance
            IconButton(
                onClick = { showDeleteDialog = true },
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "End Streak",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Confirmation dialog — iOS: confirmationDialog("End Streak with \(username)?")
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("End Streak with ${friendStreak.friend.username}?") },
            text = {
                Text(
                    "This will permanently delete your streak of " +
                        "${friendStreak.currentStreak} days. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("End Streak", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
