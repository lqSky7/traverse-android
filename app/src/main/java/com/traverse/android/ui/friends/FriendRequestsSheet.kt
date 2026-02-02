package com.traverse.android.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traverse.android.data.FriendRequest
import com.traverse.android.data.FriendStreakRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Pastel colors
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)
private val GoldColor = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsSheet(
    receivedRequests: List<FriendRequest>,
    sentRequests: List<FriendRequest>,
    receivedStreakRequests: List<FriendStreakRequest>,
    sentStreakRequests: List<FriendStreakRequest>,
    processingId: Int?,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit,
    onCancel: (FriendRequest) -> Unit,
    onAcceptStreak: (FriendStreakRequest) -> Unit,
    onRejectStreak: (FriendStreakRequest) -> Unit,
    onCancelStreak: (FriendStreakRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = 600.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp)
        ) {
            // Title
            Text(
                text = "Friend & Streak Requests",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Tab row - 4 tabs now
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentPastel,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text("Friends (${receivedRequests.size})") 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text("Sent (${sentRequests.size})") 
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { 
                        Text("Streaks (${receivedStreakRequests.size})") 
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { 
                        Text("Streak Sent (${sentStreakRequests.size})") 
                    }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> ReceivedRequestsList(
                    requests = receivedRequests,
                    processingId = processingId,
                    onAccept = onAccept,
                    onReject = onReject
                )
                1 -> SentRequestsList(
                    requests = sentRequests,
                    processingId = processingId,
                    onCancel = onCancel
                )
                2 -> ReceivedStreakRequestsList(
                    requests = receivedStreakRequests,
                    processingId = processingId,
                    onAccept = onAcceptStreak,
                    onReject = onRejectStreak
                )
                3 -> SentStreakRequestsList(
                    requests = sentStreakRequests,
                    processingId = processingId,
                    onCancel = onCancelStreak
                )
            }
        }
    }
}

@Composable
private fun ReceivedRequestsList(
    requests: List<FriendRequest>,
    processingId: Int?,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyRequestsContent(
            icon = Icons.Default.Inbox,
            message = "No received requests"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                ReceivedRequestCard(
                    request = request,
                    isProcessing = processingId == request.id,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request) }
                )
            }
        }
    }
}

@Composable
private fun ReceivedRequestCard(
    request: FriendRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val requester = request.requester ?: return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentPastel, EasyPastel)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = requester.username.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                
                // User details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requester.username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
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
                                text = "${requester.currentStreak}",
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
                                text = "${requester.totalXp} XP",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                    
                    // Time ago
                    Text(
                        text = formatRelativeTime(request.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EasyPastel,
                        contentColor = Color.Black
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Accept")
                    }
                }
                
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HardPastel
                    )
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun SentRequestsList(
    requests: List<FriendRequest>,
    processingId: Int?,
    onCancel: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyRequestsContent(
            icon = Icons.Default.Send,
            message = "No sent requests"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                SentRequestCard(
                    request = request,
                    isProcessing = processingId == request.id,
                    onCancel = { onCancel(request) }
                )
            }
        }
    }
}

@Composable
private fun SentRequestCard(
    request: FriendRequest,
    isProcessing: Boolean,
    onCancel: () -> Unit
) {
    val addressee = request.addressee ?: return
    var showCancelDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MediumPastel, AccentPastel)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = addressee.username.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            
            // User details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = addressee.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
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
                            text = "${addressee.currentStreak}",
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
                            text = "${addressee.totalXp} XP",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Pending status with time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Pending - ${formatRelativeTime(request.createdAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Cancel button
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = { showCancelDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel request",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Friend Request") },
            text = { Text("Are you sure you want to cancel the friend request to ${addressee.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    }
                ) {
                    Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Request")
                }
            }
        )
    }
}

@Composable
private fun EmptyRequestsContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}

private fun formatRelativeTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)
        
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

// MARK: - Streak Request Components

@Composable
private fun ReceivedStreakRequestsList(
    requests: List<FriendStreakRequest>,
    processingId: Int?,
    onAccept: (FriendStreakRequest) -> Unit,
    onReject: (FriendStreakRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyRequestsContent(
            icon = Icons.Default.LocalFireDepartment,
            message = "No streak requests received"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                ReceivedStreakRequestCard(
                    request = request,
                    isProcessing = processingId == request.id,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request) }
                )
            }
        }
    }
}

@Composable
private fun ReceivedStreakRequestCard(
    request: FriendStreakRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val requester = request.requester ?: return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar with flame border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HardPastel, GoldColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // User details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requester.username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    
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
                            text = "${requester.currentStreak} day streak",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                    
                    // Time ago
                    Text(
                        text = "Wants to streak together - ${formatRelativeTime(request.createdAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HardPastel,
                        contentColor = Color.White
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Start Streak")
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
private fun SentStreakRequestsList(
    requests: List<FriendStreakRequest>,
    processingId: Int?,
    onCancel: (FriendStreakRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyRequestsContent(
            icon = Icons.Default.Send,
            message = "No streak requests sent"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                SentStreakRequestCard(
                    request = request,
                    isProcessing = processingId == request.id,
                    onCancel = { onCancel(request) }
                )
            }
        }
    }
}

@Composable
private fun SentStreakRequestCard(
    request: FriendStreakRequest,
    isProcessing: Boolean,
    onCancel: () -> Unit
) {
    val requested = request.requested ?: return
    var showCancelDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with flame border
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HardPastel.copy(alpha = 0.7f), GoldColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // User details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = requested.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                
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
                        text = "${requested.currentStreak} day streak",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                // Pending status with time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Streak request pending - ${formatRelativeTime(request.createdAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Cancel button
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = { showCancelDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel streak request",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Streak Request") },
            text = { Text("Are you sure you want to cancel the streak request to ${requested.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    }
                ) {
                    Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Request")
                }
            }
        )
    }
}
