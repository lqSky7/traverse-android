package com.traverse.android.ui.friends

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Pastel colors
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)
private val GoldColor = Color(0xFFFFD700)

enum class FriendshipStatus {
    CURRENT_USER,
    NOT_FRIENDS,
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED
}

enum class FriendStreakStatus {
    NONE,
    ACTIVE,
    CAN_START,
    REQUEST_SENT,
    REQUEST_RECEIVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    val cacheManager = remember { CacheManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // State
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var statistics by remember { mutableStateOf<UserStatisticsData?>(null) }
    var friendshipStatus by remember { mutableStateOf(FriendshipStatus.NOT_FRIENDS) }
    var isActionLoading by remember { mutableStateOf(false) }
    
    // Friend Streak State
    var friendStreakStatus by remember { mutableStateOf(FriendStreakStatus.NONE) }
    var activeFriendStreak by remember { mutableStateOf<FriendStreak?>(null) }
    
    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }
    var solves by remember { mutableStateOf<List<Solve>>(emptyList()) }
    var achievements by remember { mutableStateOf<List<FriendAchievement>>(emptyList()) }
    var isSolvesLoading by remember { mutableStateOf(false) }
    var isAchievementsLoading by remember { mutableStateOf(false) }
    
    // Gift freeze state
    var isGiftingFreeze by remember { mutableStateOf(false) }
    var giftSuccessMessage by remember { mutableStateOf<String?>(null) }
    
    // Load profile data
    LaunchedEffect(username) {
        isLoading = true
        errorMessage = null
        
        // Try cache first for profile
        cacheManager.getUserProfile(username)?.let {
            profile = it.user
        }
        
        // Also try to load friendship status from cached friends list
        cacheManager.getFriends()?.let { cachedFriends ->
            val isFriend = cachedFriends.friends.any { it.username == username }
            if (isFriend) {
                friendshipStatus = FriendshipStatus.FRIENDS
                isLoading = false
            }
        }
        
        // Load from network
        try {
            scope.launch {
                val profileDeferred = async { networkService.getUserProfile(username) }
                val statsDeferred = async { networkService.getUserStatistics(username) }
                val friendsDeferred = async { networkService.getFriends() }
                val receivedDeferred = async { networkService.getReceivedFriendRequests() }
                val sentDeferred = async { networkService.getSentFriendRequests() }
                val friendStreaksDeferred = async { networkService.getFriendStreaks() }
                val receivedStreakDeferred = async { networkService.getReceivedFriendStreakRequests() }
                val sentStreakDeferred = async { networkService.getSentFriendStreakRequests() }
                
                val profileResult = profileDeferred.await()
                val statsResult = statsDeferred.await()
                val friendsResult = friendsDeferred.await()
                val receivedResult = receivedDeferred.await()
                val sentResult = sentDeferred.await()
                val friendStreaksResult = friendStreaksDeferred.await()
                val receivedStreakResult = receivedStreakDeferred.await()
                val sentStreakResult = sentStreakDeferred.await()
                
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
                    is NetworkResult.Success -> {
                        statistics = statsResult.data.stats
                    }
                    is NetworkResult.Error -> {
                        // Silently fail for stats
                    }
                }
                
                // Determine friendship status
                val friends = (friendsResult as? NetworkResult.Success)?.data?.friends ?: emptyList()
                val received = (receivedResult as? NetworkResult.Success)?.data?.requests ?: emptyList()
                val sent = (sentResult as? NetworkResult.Success)?.data?.requests ?: emptyList()
                
                friendshipStatus = when {
                    friends.any { it.username == username } -> FriendshipStatus.FRIENDS
                    received.any { it.requester?.username == username } -> FriendshipStatus.REQUEST_RECEIVED
                    sent.any { it.addressee?.username == username } -> FriendshipStatus.REQUEST_SENT
                    else -> FriendshipStatus.NOT_FRIENDS
                }
                
                // Determine friend streak status
                if (friendshipStatus == FriendshipStatus.FRIENDS) {
                    val friendStreaks = (friendStreaksResult as? NetworkResult.Success)?.data?.streaks ?: emptyList()
                    val receivedStreakRequests = (receivedStreakResult as? NetworkResult.Success)?.data?.requests ?: emptyList()
                    val sentStreakRequests = (sentStreakResult as? NetworkResult.Success)?.data?.requests ?: emptyList()
                    
                    friendStreakStatus = when {
                        friendStreaks.any { it.friend.username == username } -> {
                            activeFriendStreak = friendStreaks.find { it.friend.username == username }
                            FriendStreakStatus.ACTIVE
                        }
                        receivedStreakRequests.any { it.requester?.username == username } -> FriendStreakStatus.REQUEST_RECEIVED
                        sentStreakRequests.any { it.requested?.username == username } -> FriendStreakStatus.REQUEST_SENT
                        else -> FriendStreakStatus.CAN_START
                    }
                } else {
                    friendStreakStatus = FriendStreakStatus.NONE
                    activeFriendStreak = null
                }
                
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }
    
    // Load solves when tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && solves.isEmpty() && friendshipStatus == FriendshipStatus.FRIENDS) {
            // Try cache first
            cacheManager.getFriendSolves(username)?.let {
                solves = it.solves
            }
            
            // Then fetch from network
            isSolvesLoading = true
            when (val result = networkService.getFriendSolves(username)) {
                is NetworkResult.Success -> {
                    solves = result.data.solves
                    cacheManager.cacheFriendSolves(username, result.data)
                }
                is NetworkResult.Error -> {
                    // Silently fail
                }
            }
            isSolvesLoading = false
        } else if (selectedTab == 1 && achievements.isEmpty() && friendshipStatus == FriendshipStatus.FRIENDS) {
            // Try cache first
            cacheManager.getFriendAchievements(username)?.let {
                achievements = it.achievements
            }
            
            // Then fetch from network
            isAchievementsLoading = true
            when (val result = networkService.getFriendAchievements(username)) {
                is NetworkResult.Success -> {
                    achievements = result.data.achievements
                    cacheManager.cacheFriendAchievements(username, result.data)
                }
                is NetworkResult.Error -> {
                    // Silently fail
                }
            }
            isAchievementsLoading = false
        }
    }
    
    // Actions
    fun sendFriendRequest() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.sendFriendRequest(username)) {
                is NetworkResult.Success -> {
                    friendshipStatus = FriendshipStatus.REQUEST_SENT
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                }
            }
            isActionLoading = false
        }
    }
    
    fun removeFriend() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.removeFriend(username)) {
                is NetworkResult.Success -> {
                    friendshipStatus = FriendshipStatus.NOT_FRIENDS
                    solves = emptyList()
                    achievements = emptyList()
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                }
            }
            isActionLoading = false
        }
    }
    
    // Send friend streak request
    fun sendFriendStreakRequest() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.sendFriendStreakRequest(SendFriendStreakRequestBody(username))) {
                is NetworkResult.Success -> {
                    friendStreakStatus = FriendStreakStatus.REQUEST_SENT
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                }
            }
            isActionLoading = false
        }
    }
    
    // Delete friend streak
    fun deleteFriendStreak() {
        scope.launch {
            isActionLoading = true
            when (val result = networkService.deleteFriendStreak(username)) {
                is NetworkResult.Success -> {
                    friendStreakStatus = FriendStreakStatus.CAN_START
                    activeFriendStreak = null
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                }
            }
            isActionLoading = false
        }
    }
    
    fun giftFreeze() {
        scope.launch {
            isGiftingFreeze = true
            when (val result = networkService.giftFreezes(username, 1)) {
                is NetworkResult.Success -> {
                    giftSuccessMessage = "Freeze gifted to $username!"
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                }
            }
            isGiftingFreeze = false
        }
    }
    
    // Show success message
    LaunchedEffect(giftSuccessMessage) {
        giftSuccessMessage?.let {
            // Auto-clear success message after 3 seconds
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
    
    Scaffold(
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
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                profile != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Header
                        item {
                            ProfileHeader(
                                profile = profile!!,
                                statistics = statistics
                            )
                        }
                        
                        // Friend Action Button
                        item {
                            FriendActionButton(
                                friendshipStatus = friendshipStatus,
                                friendStreakStatus = friendStreakStatus,
                                activeFriendStreak = activeFriendStreak,
                                isLoading = isActionLoading || isGiftingFreeze,
                                onSendRequest = { sendFriendRequest() },
                                onRemoveFriend = { removeFriend() },
                                onGiftFreeze = { giftFreeze() },
                                onSendStreakRequest = { sendFriendStreakRequest() },
                                onDeleteStreak = { deleteFriendStreak() }
                            )
                        }
                        
                        // Statistics Card
                        if (statistics != null) {
                            item {
                                StatisticsCard(statistics = statistics!!)
                            }
                        }
                        
                        // Tab Row (only for friends)
                        if (friendshipStatus == FriendshipStatus.FRIENDS) {
                            item {
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Transparent,
                                    contentColor = AccentPastel
                                ) {
                                    Tab(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        text = { Text("Solves") }
                                    )
                                    Tab(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        text = { Text("Achievements") }
                                    )
                                }
                            }
                            
                            // Tab content
                            when (selectedTab) {
                                0 -> {
                                    if (isSolvesLoading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    } else if (solves.isEmpty()) {
                                        item {
                                            EmptyTabContent(
                                                icon = Icons.Default.Code,
                                                message = "No solves yet"
                                            )
                                        }
                                    } else {
                                        items(solves.take(10), key = { it.id }) { solve ->
                                            SolveRow(solve = solve)
                                        }
                                    }
                                }
                                1 -> {
                                    if (isAchievementsLoading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    } else if (achievements.isEmpty()) {
                                        item {
                                            EmptyTabContent(
                                                icon = Icons.Default.EmojiEvents,
                                                message = "No achievements yet"
                                            )
                                        }
                                    } else {
                                        items(achievements, key = { it.id }) { achievement ->
                                            AchievementRow(achievement = achievement)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            
            // Success message overlay
            giftSuccessMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EasyPastel)
                        .padding(16.dp)
                ) {
                    Row(
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
                            colors = listOf(AccentPastel, EasyPastel)
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
            
            // Visibility badge
            Text(
                text = profile.visibility.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${profile.currentStreak}",
                    label = "Streak",
                    color = HardPastel
                )
                StatColumn(
                    icon = Icons.Default.Star,
                    value = "${profile.totalXp}",
                    label = "XP",
                    color = GoldColor
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun FriendActionButton(
    friendshipStatus: FriendshipStatus,
    friendStreakStatus: FriendStreakStatus,
    activeFriendStreak: FriendStreak?,
    isLoading: Boolean,
    onSendRequest: () -> Unit,
    onRemoveFriend: () -> Unit,
    onGiftFreeze: (() -> Unit)? = null,
    onSendStreakRequest: (() -> Unit)? = null,
    onDeleteStreak: (() -> Unit)? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showGiftDialog by remember { mutableStateOf(false) }
    
    when (friendshipStatus) {
        FriendshipStatus.CURRENT_USER -> {
            // Don't show anything for current user
        }
        
        FriendshipStatus.NOT_FRIENDS -> {
            Button(
                onClick = onSendRequest,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPastel,
                    contentColor = Color.Black
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
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
        
        FriendshipStatus.REQUEST_SENT -> {
            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Pending")
            }
        }
        
        FriendshipStatus.REQUEST_RECEIVED -> {
            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Received - Check Requests")
            }
        }
        
        FriendshipStatus.FRIENDS -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Friend Streak Section
                when (friendStreakStatus) {
                    FriendStreakStatus.CAN_START -> {
                        Button(
                            onClick = { onSendStreakRequest?.invoke() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HardPastel,
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Streak Together")
                            }
                        }
                    }
                    FriendStreakStatus.REQUEST_SENT -> {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HardPastel.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Streak Request Pending")
                        }
                    }
                    FriendStreakStatus.REQUEST_RECEIVED -> {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HardPastel
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Streak Request Received")
                        }
                    }
                    FriendStreakStatus.ACTIVE -> {
                        // Active streak card
                        if (activeFriendStreak != null) {
                            ActiveStreakCard(
                                friendStreak = activeFriendStreak,
                                onDelete = { onDeleteStreak?.invoke() },
                                isLoading = isLoading
                            )
                        }
                    }
                    FriendStreakStatus.NONE -> {
                        // No streak section when not friends
                    }
                }
                
                // Gift Freeze Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable(
                            enabled = !isLoading,
                            onClick = { showGiftDialog = true }
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.Cyan
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Cyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Gift Freeze (70 XP)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.Cyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
                
                // Remove Friend Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable(
                            enabled = !isLoading,
                            onClick = { showRemoveDialog = true }
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = HardPastel
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = HardPastel
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Remove Friend",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = HardPastel,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Remove Friend Confirmation Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove this friend?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemoveFriend()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
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
            text = { Text("This will cost 70 XP from your balance. Your friend can use it to protect their streak!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiftDialog = false
                        onGiftFreeze?.invoke()
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
}

@Composable
private fun StatisticsCard(statistics: UserStatisticsData) {
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
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(value = "${statistics.totalSolves}", label = "Solves", modifier = Modifier.weight(1f))
                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                StatItem(value = "${statistics.totalStreakDays}", label = "Streak Days", modifier = Modifier.weight(1f))
                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                StatItem(value = "${statistics.totalSubmissions}", label = "Submissions", modifier = Modifier.weight(1f))
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Difficulty breakdown
            Text(
                text = "By Difficulty",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DifficultyChip(
                    label = "Easy",
                    count = statistics.problemsByDifficulty.easy,
                    color = EasyPastel
                )
                DifficultyChip(
                    label = "Medium",
                    count = statistics.problemsByDifficulty.medium,
                    color = MediumPastel
                )
                DifficultyChip(
                    label = "Hard",
                    count = statistics.problemsByDifficulty.hard,
                    color = HardPastel
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun DifficultyChip(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f)
            )
        )
    }
}

@Composable
private fun SolveRow(solve: Solve) {
    val difficultyColor = when (solve.problem.difficulty.lowercase()) {
        "easy" -> EasyPastel
        "medium" -> MediumPastel
        "hard" -> HardPastel
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(difficultyColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = solve.problem.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${solve.problem.platform.replaceFirstChar { it.uppercase() }} - ${solve.submission.language}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            
            // XP
            Text(
                text = "+${solve.xpAwarded} XP",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GoldColor
                )
            )
        }
    }
}

@Composable
private fun AchievementRow(achievement: FriendAchievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Trophy icon
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyTabContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ActiveStreakCard(
    friendStreak: FriendStreak,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = HardPastel,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Streak with ${friendStreak.friend.username}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete streak",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${friendStreak.currentStreak}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HardPastel
                        )
                    )
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                // Vertical separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${friendStreak.longestStreak}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldColor
                        )
                    )
                    Text(
                        text = "Best Streak",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("End Streak") },
            text = { Text("Are you sure you want to end your streak with ${friendStreak.friend.username}? This cannot be undone.") },
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
                    Text("Keep Streak")
                }
            }
        )
    }
}
