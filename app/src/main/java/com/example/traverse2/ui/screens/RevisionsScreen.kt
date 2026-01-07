package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

// Data classes for revisions
data class RevisionStats(
    val dueToday: Int,
    val overdue: Int,
    val completed: Int,
    val total: Int
)

data class RevisionItem(
    val id: Int,
    val problemTitle: String,
    val platform: String,
    val difficulty: String,
    val revisionNumber: Int,
    val scheduledFor: String,
    val isOverdue: Boolean,
    val isCompleted: Boolean
)

data class RevisionGroup(
    val date: String,
    val displayDate: String,
    val revisions: List<RevisionItem>,
    val isToday: Boolean,
    val isOverdue: Boolean
)

@Composable
fun RevisionsScreen(
    hazeState: HazeState
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    
    // Mock data - would come from API
    val stats = RevisionStats(
        dueToday = 5,
        overdue = 2,
        completed = 47,
        total = 54
    )
    
    val revisionGroups = listOf(
        RevisionGroup(
            date = "2026-01-01",
            displayDate = "Today",
            isToday = true,
            isOverdue = false,
            revisions = listOf(
                RevisionItem(1, "Two Sum", "LeetCode", "Easy", 1, "2026-01-01", false, false),
                RevisionItem(2, "Valid Parentheses", "LeetCode", "Easy", 2, "2026-01-01", false, false),
                RevisionItem(3, "Merge Intervals", "LeetCode", "Medium", 1, "2026-01-01", false, true),
                RevisionItem(4, "LRU Cache", "LeetCode", "Hard", 1, "2026-01-01", false, false),
                RevisionItem(5, "Binary Search", "LeetCode", "Easy", 3, "2026-01-01", false, false)
            )
        ),
        RevisionGroup(
            date = "2025-12-31",
            displayDate = "Yesterday",
            isToday = false,
            isOverdue = true,
            revisions = listOf(
                RevisionItem(6, "Three Sum", "LeetCode", "Medium", 1, "2025-12-31", true, false),
                RevisionItem(7, "Graph Clone", "LeetCode", "Medium", 2, "2025-12-31", true, false)
            )
        ),
        RevisionGroup(
            date = "2026-01-02",
            displayDate = "Tomorrow",
            isToday = false,
            isOverdue = false,
            revisions = listOf(
                RevisionItem(8, "Longest Substring", "LeetCode", "Medium", 1, "2026-01-02", false, false),
                RevisionItem(9, "Container With Water", "LeetCode", "Medium", 2, "2026-01-02", false, false)
            )
        ),
        RevisionGroup(
            date = "2026-01-03",
            displayDate = "Jan 3, 2026",
            isToday = false,
            isOverdue = false,
            revisions = listOf(
                RevisionItem(10, "Word Search", "LeetCode", "Medium", 1, "2026-01-03", false, false)
            )
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp, bottom = 120.dp)
    ) {
        // Header
        Text(
            text = "Revisions",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = glassColors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Track your spaced repetition progress",
            fontSize = 14.sp,
            color = glassColors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats Card - Rectangular with more rounded corners
        StatsCard(
            hazeState = hazeState,
            glassColors = glassColors,
            stats = stats
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Section Header
        Text(
            text = "Schedule",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = glassColors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Groups
        revisionGroups.forEachIndexed { index, group ->
            DateGroupCard(
                hazeState = hazeState,
                glassColors = glassColors,
                group = group,
                delayMs = index * 100
            )
            
            if (index < revisionGroups.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    stats: RevisionStats
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    // Animate the stats
    val dueTodayAnim = remember { Animatable(0f) }
    val overdueAnim = remember { Animatable(0f) }
    val completedAnim = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        dueTodayAnim.animateTo(stats.dueToday.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(100)
        overdueAnim.animateTo(stats.overdue.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(200)
        completedAnim.animateTo(stats.completed.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Schedule,
                value = dueTodayAnim.value.toInt().toString(),
                label = "Due Today",
                color = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                glassColors = glassColors
            )
            
            StatItem(
                icon = Icons.Default.Warning,
                value = overdueAnim.value.toInt().toString(),
                label = "Overdue",
                color = Color(0xFFEF4444),
                glassColors = glassColors
            )
            
            StatItem(
                icon = Icons.Default.CheckCircle,
                value = completedAnim.value.toInt().toString(),
                label = "Done",
                color = Color(0xFF22C55E),
                glassColors = glassColors
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    glassColors: GlassColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (glassColors.isDark) Color(0xFFCCCCCC) else glassColors.textSecondary
        )
    }
}

@Composable
private fun DateGroupCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    group: RevisionGroup,
    delayMs: Int
) {
    var isExpanded by remember { mutableStateOf(group.isToday || group.isOverdue) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cardAlpha"
    )
    
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    // Determine header color based on status
    val headerColor = when {
        group.isOverdue -> Color(0xFFEF4444)
        group.isToday -> if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        else -> glassColors.textSecondary
    }
    
    val pendingCount = group.revisions.count { !it.isCompleted }
    val completedCount = group.revisions.count { it.isCompleted }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .clickable { isExpanded = !isExpanded }
            .padding(20.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = headerColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = group.displayDate,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary
                    )
                    
                    if (group.isOverdue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OVERDUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Count badge
                    Text(
                        text = if (completedCount > 0) "$pendingCount/${ group.revisions.size}" else "${group.revisions.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = glassColors.textSecondary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = glassColors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    group.revisions.forEachIndexed { index, revision ->
                        RevisionListItem(
                            revision = revision,
                            glassColors = glassColors
                        )
                        
                        if (index < group.revisions.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionListItem(
    revision: RevisionItem,
    glassColors: GlassColors
) {
    val difficultyColor = when (revision.difficulty.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    val bgColor = when {
        revision.isCompleted -> if (glassColors.isDark) Color(0x10FFFFFF) else Color(0x08000000)
        revision.isOverdue -> Color(0x15EF4444)
        else -> if (glassColors.isDark) Color(0x18FFFFFF) else Color(0x15000000)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { /* TODO: Mark as complete */ }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Completion indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (revision.isCompleted) Color(0xFF22C55E).copy(alpha = 0.2f)
                        else glassColors.textSecondary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (revision.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "${revision.revisionNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = revision.problemTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (revision.isCompleted) 
                        glassColors.textSecondary 
                    else 
                        glassColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = revision.platform,
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                    
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                    
                    Text(
                        text = "Rev #${revision.revisionNumber}",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                }
            }
        }
        
        // Difficulty badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(difficultyColor.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = revision.difficulty,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = difficultyColor
            )
        }
    }
}
