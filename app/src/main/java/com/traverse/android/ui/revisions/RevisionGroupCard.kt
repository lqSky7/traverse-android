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

// Pastel colors matching Android app's monochromish-pastel theme
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

@Composable
fun RevisionGroupCard(
    group: RevisionGroup,
    useMLMode: Boolean,
    completingId: Int?,
    onComplete: (Int) -> Unit,
    onDelete: (Int) -> Unit,
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
                        useMLMode = useMLMode,
                        isCompleting = completingId == revision.id,
                        onComplete = { onComplete(revision.id) },
                        onDelete = { onDelete(revision.id) }
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
    useMLMode: Boolean,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showMLSheet by remember { mutableStateOf(false) }
    
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
                
                useMLMode -> {
                    IconButton(
                        onClick = { showMLSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "ML Attempt",
                            tint = buttonColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                else -> {
                    IconButton(
                        onClick = onComplete,
                        enabled = !isCompleting
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = "Complete",
                                tint = buttonColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            // Context menu for ML mode
            if (useMLMode && !revision.isCompleted) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Revision") },
                        onClick = {
                            onDelete()
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
    
    // ML Attempt Sheet
    if (showMLSheet) {
        MLAttemptSheet(
            revision = revision,
            onDismiss = { showMLSheet = false },
            onOpenProblem = { 
                openProblemUrl(context, revision.problem.platform, revision.problem.slug)
                showMLSheet = false
            }
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
