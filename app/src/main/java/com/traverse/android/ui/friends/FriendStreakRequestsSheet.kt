package com.traverse.android.ui.friends

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.DataManager
import com.traverse.android.data.FriendStreakRequest
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import kotlinx.coroutines.launch

/**
 * 1:1 Kotlin port of iOS FriendStreakRequestsView.swift.
 * Displays received and sent friend streak invites with Accept, Decline, and Cancel actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendStreakRequestsSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataManager = remember { DataManager.getInstance(context) }
    val networkService = remember { NetworkService.getInstance(context) }

    val receivedRequests by dataManager.receivedStreakRequests.collectAsState()
    val sentRequests by dataManager.sentStreakRequests.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Received, 1: Sent
    var actionInProgressId by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141824),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF6D00),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Streak Requests",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Text("Done", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2230), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StreakTabButton(
                    title = "Received (${receivedRequests.size})",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                StreakTabButton(
                    title = "Sent (${sentRequests.size})",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTab == 0) receivedRequests else sentRequests

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 0) "No received streak requests" else "No sent streak requests",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(currentList, key = { it.id }) { request ->
                        StreakRequestCard(
                            request = request,
                            isReceived = selectedTab == 0,
                            isProcessing = actionInProgressId == request.id,
                            onAccept = {
                                actionInProgressId = request.id
                                scope.launch {
                                    val res = networkService.acceptFriendStreakRequest(request.id)
                                    actionInProgressId = null
                                    if (res is NetworkResult.Success) {
                                        Toast.makeText(context, "Streak request accepted!", Toast.LENGTH_SHORT).show()
                                        val curUser = dataManager.userStats.value?.username ?: ""
                                        dataManager.fetchAllData(curUser)
                                    }
                                }
                            },
                            onDecline = {
                                actionInProgressId = request.id
                                scope.launch {
                                    val res = networkService.rejectFriendStreakRequest(request.id)
                                    actionInProgressId = null
                                    if (res is NetworkResult.Success) {
                                        val curUser = dataManager.userStats.value?.username ?: ""
                                        dataManager.fetchAllData(curUser)
                                    }
                                }
                            },
                            onCancel = {
                                actionInProgressId = request.id
                                scope.launch {
                                    val res = networkService.cancelFriendStreakRequest(request.id)
                                    actionInProgressId = null
                                    if (res is NetworkResult.Success) {
                                        val curUser = dataManager.userStats.value?.username ?: ""
                                        dataManager.fetchAllData(curUser)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakRequestCard(
    request: FriendStreakRequest,
    isReceived: Boolean,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val username = if (isReceived) request.requester?.username ?: "Unknown" else request.requested?.username ?: "Unknown"
    val streak = if (isReceived) request.requester?.currentStreak ?: 0 else request.requested?.currentStreak ?: 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E2230),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6D00).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with flame badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFF6D00).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    color = Color(0xFFFF6D00),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streak day streak",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6D00),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (isReceived) {
                // Accept / Decline
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Decline",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF6D00).copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Cancel
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color(0xFFFF5252), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StreakTabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF2B3145) else Color.Transparent
    ) {
        Text(
            text = title,
            color = if (selected) Color(0xFFFF6D00) else Color.White.copy(alpha = 0.5f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
