package com.traverse.android.ui.revisions

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.Revision
import com.traverse.android.data.RevisionGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val PurplePastel = Color(0xFFC084FC)
private val CardBackground = Color(0xFF1A1A1A)

@Composable
fun RevisionGroupCard(
    group: RevisionGroup,
    completingId: Int?,
    onComplete: (Int) -> Unit,
    onDeleteSingle: (Int) -> Unit,
    onDeleteProblem: (Int) -> Unit,
    onRescheduleDays: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val groupDate = group.displayDate

    val (dateIcon, dateColor) = when {
        groupDate == today -> Icons.Default.Schedule to AccentPastel
        groupDate == today.plusDays(1) -> Icons.Default.Event to AccentPastel
        groupDate.isBefore(today) -> Icons.Default.Warning to HardPastel
        else -> Icons.Default.CalendarMonth to Color.White.copy(alpha = 0.5f)
    }

    val formattedDate = remember(group.date) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
        groupDate.format(formatter).uppercase()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = dateIcon,
                contentDescription = null,
                tint = dateColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${group.count}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = dateColor
                )
            )
        }

        // Revisions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                group.revisions.forEachIndexed { index, revision ->
                    RevisionItem(
                        revision = revision,
                        isCompleting = completingId == revision.id,
                        onComplete = { onComplete(revision.id) },
                        onDeleteSingle = { onDeleteSingle(revision.id) },
                        onDeleteProblem = { onDeleteProblem(revision.problem.id) },
                        onRescheduleDays = { days -> onRescheduleDays(revision.id, days) }
                    )

                    // Divider between items
                    if (index < group.revisions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevisionItem(
    revision: Revision,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onDeleteSingle: () -> Unit,
    onDeleteProblem: () -> Unit,
    onRescheduleDays: (Int) -> Unit
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showCoachSheet by remember { mutableStateOf(false) }
    var showSchedulingInfoSheet by remember { mutableStateOf(false) }
    var showRescheduleSheet by remember { mutableStateOf(false) }
    var showCodeHistorySheet by remember { mutableStateOf(false) }

    val difficultyColor = when (revision.problem.difficulty.lowercase()) {
        "easy" -> EasyPastel
        "medium" -> MediumPastel
        "hard" -> HardPastel
        else -> Color.Gray
    }

    val buttonColor = if (revision.isOverdue) HardPastel else AccentPastel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showContextMenu = true }
            .alpha(if (revision.isCompleted) 0.6f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Difficulty indicator bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(difficultyColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Problem info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = revision.problem.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = revision.problem.platform.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = " \u2022 ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.4f)
                    )
                )
                Text(
                    text = "Revision #${revision.revisionNumber}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action button
        Box {
            when {
                revision.isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EasyPastel,
                        modifier = Modifier.size(28.dp)
                    )
                }

                else -> {
                    IconButton(
                        onClick = { showCoachSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach & Hints",
                            tint = PurplePastel,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Context menu matching iOS 1:1
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                containerColor = CardBackground
            ) {
                // 1. AI Revision Coach & Hints
                DropdownMenuItem(
                    text = { Text("AI Revision Coach & Hints", color = PurplePastel, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        showCoachSheet = true
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PurplePastel
                        )
                    }
                )

                // 2. Open Problem
                DropdownMenuItem(
                    text = { Text("Open Problem", color = Color.White) },
                    onClick = {
                        openProblemUrl(context, revision.problem.platform, revision.problem.slug)
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF00E676)
                        )
                    }
                )

                // 3. Attempt History
                DropdownMenuItem(
                    text = { Text("Attempt History", color = Color.White) },
                    onClick = {
                        showCodeHistorySheet = true
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = AccentPastel
                        )
                    }
                )

                // 4. ML Scheduling Details
                DropdownMenuItem(
                    text = { Text("FSRS Spaced Repetition", color = Color.White) },
                    onClick = {
                        showSchedulingInfoSheet = true
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MediumPastel
                        )
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                if (!revision.isCompleted) {
                    // 5. Reschedule 7 Days Later
                    DropdownMenuItem(
                        text = { Text("Reschedule 7 Days Later", color = Color.White) },
                        onClick = {
                            onRescheduleDays(7)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AccentPastel
                            )
                        }
                    )

                    // 6. Reschedule 14 Days Later
                    DropdownMenuItem(
                        text = { Text("Reschedule 14 Days Later", color = Color.White) },
                        onClick = {
                            onRescheduleDays(14)
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = AccentPastel
                            )
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // 7. Remove from Revision List (Destructive)
                    DropdownMenuItem(
                        text = { Text("Remove from Revision List", color = HardPastel) },
                        onClick = {
                            onDeleteProblem()
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = HardPastel
                            )
                        }
                    )

                    // 8. Delete Single Revision (Destructive)
                    DropdownMenuItem(
                        text = { Text("Delete Single ML Revision", color = HardPastel) },
                        onClick = {
                            onDeleteSingle()
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = HardPastel
                            )
                        }
                    )
                }
            }
        }
    }

    // AI Revision Coach Sheet
    if (showCoachSheet) {
        RevisionCoachSheet(
            revision = revision,
            onDismiss = { showCoachSheet = false }
        )
    }

    // FSRS Spaced Repetition Info Sheet
    if (showSchedulingInfoSheet) {
        MLSchedulingInfoSheet(
            onDismiss = { showSchedulingInfoSheet = false }
        )
    }

    // Reschedule Sheet
    if (showRescheduleSheet) {
        RescheduleSheet(
            revision = revision,
            onDismiss = { showRescheduleSheet = false },
            onRescheduled = { /* ViewModel refreshes */ }
        )
    }

    // Code Attempt History Sheet
    if (showCodeHistorySheet) {
        AttemptCodeHistorySheet(
            revision = revision,
            onDismiss = { showCodeHistorySheet = false }
        )
    }
}

private fun openProblemUrl(context: android.content.Context, platform: String, slug: String) {
    val baseUrls = mapOf(
        "leetcode" to "https://leetcode.com/problems/",
        "codeforces" to "https://codeforces.com/problemset/problem/",
        "hackerrank" to "https://www.hackerrank.com/challenges/",
        "takeuforward" to "https://takeuforward.org/practice/"
    )

    val baseUrl = baseUrls[platform.lowercase()] ?: return
    val url = "$baseUrl$slug"

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}
