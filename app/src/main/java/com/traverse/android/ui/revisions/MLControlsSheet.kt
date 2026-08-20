package com.traverse.android.ui.revisions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import kotlinx.coroutines.launch

/**
 * 1:1 Kotlin port of iOS ML Spaced Repetition Controls Sheet.
 * Allows pausing schedule (vacation/exam mode), resuming schedule with backlog compression,
 * or running manual recalibration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLControlsSheet(
    isCurrentlyPaused: Boolean = false,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkService = remember { NetworkService.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(if (isCurrentlyPaused) 1 else 0) } // 0: Pause, 1: Resume, 2: Recalibrate
    var pauseDays by remember { mutableIntStateOf(7) }
    var backlogDays by remember { mutableIntStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141824),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = Color(0xFFE040FB),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Spaced Repetition Controls",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2230), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ControlTabButton(
                    title = "Pause",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                ControlTabButton(
                    title = "Resume",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
                ControlTabButton(
                    title = "Recalibrate",
                    selected = selectedTab == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> {
                    // Pause Schedule
                    Text(
                        text = "Exam Mode / Vacation Pause",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Temporarily freezes your daily review load without accumulating penalty decay.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 7, 14, 30).forEach { days ->
                            val isSel = pauseDays == days
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pauseDays = days },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFE040FB).copy(alpha = 0.2f) else Color(0xFF1E2230),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFFE040FB) else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Text(
                                    text = "$days Days",
                                    color = if (isSel) Color(0xFFE040FB) else Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                val res = networkService.pauseRevisions(pauseDays)
                                isLoading = false
                                if (res is NetworkResult.Success) {
                                    Toast.makeText(context, res.data.message, Toast.LENGTH_SHORT).show()
                                    onUpdated()
                                    onDismiss()
                                } else if (res is NetworkResult.Error) {
                                    isError = true
                                    statusMessage = res.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Pause for $pauseDays Days", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // Resume Schedule
                    Text(
                        text = "Resume & Compress Backlog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Evenly spreads accumulated overdue backlog across selected days so you're not overwhelmed.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5, 7, 14).forEach { days ->
                            val isSel = backlogDays == days
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { backlogDays = days },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF1E2230),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Text(
                                    text = "$days Days",
                                    color = if (isSel) Color(0xFF00E676) else Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                val res = networkService.resumeRevisions(backlogDays)
                                isLoading = false
                                if (res is NetworkResult.Success) {
                                    Toast.makeText(context, res.data.message, Toast.LENGTH_SHORT).show()
                                    onUpdated()
                                    onDismiss()
                                } else if (res is NetworkResult.Error) {
                                    isError = true
                                    statusMessage = res.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Resume Over $backlogDays Days", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // Manual Recalibrate
                    Text(
                        text = "Recalibrate Interval Memory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Rebalances upcoming intervals based on current retention probability and your daily review capacity.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                val res = networkService.recalibrateRevisions()
                                isLoading = false
                                if (res is NetworkResult.Success) {
                                    Toast.makeText(context, res.data.message, Toast.LENGTH_SHORT).show()
                                    onUpdated()
                                    onDismiss()
                                } else if (res is NetworkResult.Error) {
                                    isError = true
                                    statusMessage = res.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Trigger Recalibration Now", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage ?: "",
                    color = if (isError) Color(0xFFFF5252) else Color(0xFF00E676),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ControlTabButton(
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
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
