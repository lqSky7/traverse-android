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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traverse2.R
import com.example.traverse2.data.api.RevisionGroup
import com.example.traverse2.data.api.RevisionItem
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import com.example.traverse2.ui.viewmodel.RevisionType
import com.example.traverse2.ui.viewmodel.RevisionsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RevisionsScreen(
    hazeState: HazeState,
    viewModel: RevisionsViewModel = viewModel()
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    
    val currentStats = when (uiState.currentTab) {
        RevisionType.NORMAL -> uiState.normalStats
        RevisionType.ML -> uiState.mlStats
    }
    
    val currentGroups = when (uiState.currentTab) {
        RevisionType.NORMAL -> uiState.normalGroups
        RevisionType.ML -> uiState.mlGroups
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 60.dp, bottom = 120.dp)
                .blur(if (uiState.showPaywall) 8.dp else 0.dp)
        ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Revisions",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (uiState.currentTab == RevisionType.ML)
                        "ML-powered spaced repetition (LSTM)"
                    else
                        "Track your spaced repetition progress",
                    fontSize = 14.sp,
                    color = glassColors.textSecondary
                )
            }
            
            IconButton(onClick = { viewModel.loadRevisions() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = glassColors.textPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tab Selector (Normal / ML)
        if (!uiState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x20000000))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "Normal",
                    isSelected = uiState.currentTab == RevisionType.NORMAL,
                    onClick = { viewModel.switchTab(RevisionType.NORMAL) },
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
                
                TabButton(
                    text = "ML (LSTM)${if (!uiState.hasMLAccess) " 🔒" else ""}",
                    isSelected = uiState.currentTab == RevisionType.ML,
                    onClick = {
                        viewModel.switchTab(RevisionType.ML)
                    },
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = if (glassColors.isDark) Color(0xFFE91E8C) else Color(0xFFA855F7))
            }
        } else {
            // Stats Card
            currentStats?.let { stats ->
                StatsCard(
                    hazeState = hazeState,
                    glassColors = glassColors,
                    dueToday = stats.dueToday,
                    overdue = stats.overdue,
                    completed = stats.completed,
                    isML = uiState.currentTab == RevisionType.ML
                )
            }
            
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
            if (currentGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No revisions scheduled",
                            color = glassColors.textSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.currentTab == RevisionType.ML && !uiState.hasMLAccess)
                                "Premium subscription required for ML revisions"
                            else
                                "Complete problems to schedule revisions",
                            color = glassColors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                currentGroups.forEachIndexed { index, group ->
                    DateGroupCard(
                        hazeState = hazeState,
                        glassColors = glassColors,
                        group = group,
                        delayMs = index * 100,
                        isML = uiState.currentTab == RevisionType.ML,
                        onCompleteRevision = { revisionId ->
                            viewModel.completeRevision(revisionId, uiState.currentTab == RevisionType.ML)
                        }
                    )
                    
                    if (index < currentGroups.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        }
        
        // Paywall overlay
        if (uiState.showPaywall) {
            PaywallBottomSheet(
                onDismiss = { viewModel.dismissPaywall() }
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    glassColors: GlassColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    if (glassColors.isDark) Color(0xFFE91E8C) else Color(0xFFA855F7)
                } else {
                    Color.Transparent
                }
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                Color.White
            } else {
                if (enabled) glassColors.textSecondary else glassColors.textSecondary.copy(alpha = 0.4f)
            }
        )
    }
}

@Composable
private fun StatsCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    dueToday: Int,
    overdue: Int,
    completed: Int,
    isML: Boolean
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    // Animate the stats
    val dueTodayAnim = remember { Animatable(0f) }
    val overdueAnim = remember { Animatable(0f) }
    val completedAnim = remember { Animatable(0f) }
    
    LaunchedEffect(dueToday, overdue, completed) {
        dueTodayAnim.animateTo(dueToday.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(dueToday, overdue, completed) {
        delay(100)
        overdueAnim.animateTo(overdue.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(dueToday, overdue, completed) {
        delay(200)
        completedAnim.animateTo(completed.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = 24.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(
                if (isML) {
                    // ML has gradient background
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0x40E91E8C),
                            Color(0x40A855F7)
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF),
                            if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF)
                        )
                    )
                }
            )
            .padding(24.dp)
    ) {
        Column {
            if (isML) {
                Text(
                    text = "🧠 ML-Powered Scheduling",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (glassColors.isDark) Color(0xFFE91E8C) else Color(0xFFA855F7),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
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
    delayMs: Int,
    isML: Boolean,
    onCompleteRevision: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    // Parse date and determine display text
    val today = LocalDate.now()
    val groupDate = try {
        LocalDate.parse(group.date, DateTimeFormatter.ISO_DATE)
    } catch (e: Exception) {
        today
    }
    
    val displayDate = when {
        groupDate.isEqual(today) -> "Today"
        groupDate.isEqual(today.minusDays(1)) -> "Yesterday"
        groupDate.isEqual(today.plusDays(1)) -> "Tomorrow"
        else -> groupDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    
    val isToday = groupDate.isEqual(today)
    val isOverdue = groupDate.isBefore(today)
    
    // Auto-expand today and overdue
    LaunchedEffect(Unit) {
        if (isToday || isOverdue) {
            isExpanded = true
        }
    }
    
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
        isOverdue -> Color(0xFFEF4444)
        isToday -> if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        else -> glassColors.textSecondary
    }
    
    val pendingCount = group.revisions.count { it.completedAt == null }
    val completedCount = group.revisions.count { it.completedAt != null }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
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
                        text = displayDate,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary
                    )
                    
                    if (isOverdue) {
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
                    
                    if (isML) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🧠",
                            fontSize = 14.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Count badge
                    Text(
                        text = if (completedCount > 0) "$pendingCount/${group.revisions.size}" else "${group.revisions.size}",
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
                            glassColors = glassColors,
                            isML = isML,
                            hazeState = hazeState,
                            onComplete = { onCompleteRevision(revision.id) }
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
    glassColors: GlassColors,
    isML: Boolean,
    hazeState: HazeState,
    onComplete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val menuGlassStyle = HazeStyle(
        backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
        blurRadius = if (glassColors.isDark) 30.dp else 25.dp,
        tint = HazeTint(
            color = if (glassColors.isDark) 
                Color.White.copy(alpha = 0.15f) 
            else 
                Color.White.copy(alpha = 0.80f)
        ),
        noiseFactor = 0.03f
    )
    
    val difficultyColor = when (revision.problem.difficulty?.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    val isCompleted = revision.completedAt != null
    val today = LocalDate.now()
    val scheduledDate = try {
        LocalDate.parse(revision.scheduledFor.split("T")[0], DateTimeFormatter.ISO_DATE)
    } catch (e: Exception) {
        today
    }
    val isOverdue = scheduledDate.isBefore(today) && !isCompleted
    
    val bgColor = when {
        isCompleted -> if (glassColors.isDark) Color(0x10FFFFFF) else Color(0x08000000)
        isOverdue -> Color(0x15EF4444)
        else -> if (glassColors.isDark) Color(0x18FFFFFF) else Color(0x15000000)
    }
    
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .clickable(enabled = !isCompleted) { showMenu = true }
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
                        if (isCompleted) Color(0xFF22C55E).copy(alpha = 0.2f)
                        else glassColors.textSecondary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (revision.type == "ml") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            if (glassColors.isDark) Color(0xFFE91E8C) else Color(0xFFA855F7),
                                            if (glassColors.isDark) Color(0xFFA855F7) else Color(0xFFE91E8C)
                                        )
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ML",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = revision.problem.title ?: "Problem",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCompleted) 
                            glassColors.textSecondary 
                        else 
                            glassColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = revision.problem.platform,
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
        revision.problem.difficulty?.let { difficulty ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(difficultyColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = difficulty.capitalize(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = difficultyColor
                )
            }
        }
    }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .hazeChild(state = hazeState, style = menuGlassStyle)
                .border(
                    width = if (glassColors.isDark) 1.dp else 1.5.dp,
                    color = if (glassColors.isDark)
                        Color.White.copy(alpha = 0.2f)
                    else
                        Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Delete",
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    showMenu = false
                    onComplete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444)
                    )
                }
            )
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { 
    if (it.isLowerCase()) it.titlecase() else it.toString() 
}

@Composable
private fun PaywallBottomSheet(
    onDismiss: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = if (glassColors.isDark) {
                                listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF0F0F1E)
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFF5F7),
                                    Color(0xFFFFFFFF)
                                )
                            }
                        )
                    )
                    .clickable(enabled = false) { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upgrade to ML Premium",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = glassColors.textPrimary
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = glassColors.textSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Big Price in Center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "₹49",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE91E8C)
                        )
                        Text(
                            text = "per month",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = glassColors.textSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Features (Small text)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeatureItem(
                            title = "Premium ML based Revision Scheduler",
                            glassColors = glassColors
                        )
                        FeatureItem(
                            title = "All Platforms support",
                            glassColors = glassColors
                        )
                        FeatureItem(
                            title = "Early Access to features",
                            glassColors = glassColors
                        )
                        FeatureItem(
                            title = "WatchOS app support",
                            glassColors = glassColors
                        )
                        FeatureItem(
                            title = "Anki and notion sync",
                            glassColors = glassColors
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Big Upgrade Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE91E8C),
                                        Color(0xFFA855F7)
                                    )
                                )
                            )
                            .clickable {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://leet-feedback.vercel.app")
                                )
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Upgrade Now",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    glassColors: GlassColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE91E8C),
                            Color(0xFFA855F7)
                        )
                    )
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = glassColors.textSecondary
        )
    }
}
