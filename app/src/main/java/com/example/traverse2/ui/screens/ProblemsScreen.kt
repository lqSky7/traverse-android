package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.traverse2.data.api.Solve
import com.example.traverse2.ui.components.GlassTopBar
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import com.example.traverse2.ui.viewmodel.ProblemsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

private val CATEGORY_NAMES = listOf(
    "Arrays", "Strings", "LinkedList", "Trees", "Graphs",
    "DP", "Greedy", "Backtracking", "Sorting", "Searching",
    "Stack", "Queue", "Heap", "HashMap", "Math"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemsScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
    viewModel: ProblemsViewModel = viewModel()
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    val progressColor = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
    val trackColor = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x30E91E8C)
    val successColor = Color(0xFF22C55E)
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = glassColors.textPrimary)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Error: ${uiState.error}", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.clickable { viewModel.refresh() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, "Retry", tint = glassColors.textSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap to retry", color = glassColors.textSecondary)
                        }
                    }
                }
            }
            else -> {
                val solves = uiState.solves
                val stats = uiState.solveStats
                val problemsCompleted = stats?.totalSolves ?: solves.size
                val totalProblems = uiState.totalProblems.coerceAtLeast(problemsCompleted)
                val percentage = if (totalProblems > 0) (problemsCompleted.toFloat() / totalProblems * 100).toInt() else 0
                
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                            .padding(top = 100.dp, bottom = 120.dp)
                    ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats Card
                    GlassCard(hazeState = hazeState, glassColors = glassColors) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val progress = remember { Animatable(0f) }
                            LaunchedEffect(problemsCompleted, totalProblems) {
                                progress.animateTo(
                                    if (totalProblems > 0) problemsCompleted.toFloat() / totalProblems else 0f,
                                    tween(1200, easing = FastOutSlowInEasing)
                                )
                            }
                            
                            Box(
                                modifier = Modifier.size(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(trackColor, -90f, 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                    drawArc(progressColor, -90f, 360f * progress.value, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$percentage%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = glassColors.textPrimary
                                    )
                                    Text(
                                        text = "Complete",
                                        fontSize = 11.sp,
                                        color = glassColors.textSecondary
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Solved",
                                        tint = successColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$problemsCompleted",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = successColor
                                    )
                                }
                                Text(
                                    text = "Solved",
                                    fontSize = 13.sp,
                                    color = glassColors.textSecondary
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${stats?.totalXp ?: 0}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (glassColors.isDark) Color(0xFFFBBF24) else Color(0xFFE91E8C)
                                )
                                Text(
                                    text = "Total XP",
                                    fontSize = 13.sp,
                                    color = glassColors.textSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Solved Problems Section
                    if (solves.isNotEmpty()) {
                        GlassCard(hazeState = hazeState, glassColors = glassColors) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(successColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Solved Problems",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = glassColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${solves.size})",
                                        fontSize = 14.sp,
                                        color = glassColors.textSecondary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                solves.forEachIndexed { index, solve ->
                                    AnimatedSolveItem(solve, glassColors, index * 50)
                                    if (index < solves.size - 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        GlassCard(hazeState = hazeState, glassColors = glassColors) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    tint = glassColors.textSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No problems solved yet",
                                    fontSize = 16.sp,
                                    color = glassColors.textSecondary
                                )
                                Text(
                                    text = "Start solving to see your progress here",
                                    fontSize = 13.sp,
                                    color = glassColors.textSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        GlassTopBar(
            title = "Problems",
            hazeState = hazeState,
            glassColors = glassColors,
            onBack = onBack,
            icon = Icons.Default.Code,
            iconTint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
        )
    }
}

@Composable
private fun GlassCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    content: @Composable () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
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
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
private fun AnimatedSolveItem(solve: Solve, glassColors: GlassColors, delayMs: Int) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "solveItemAlpha"
    )
    
    SolveListItem(
        solve = solve,
        glassColors = glassColors,
        modifier = Modifier.alpha(alpha)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SolveListItem(
    solve: Solve,
    glassColors: GlassColors,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val difficultyColor = when (solve.problem.difficulty?.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    val bgColor = if (glassColors.isDark) Color(0x18FFFFFF) else Color(0x15000000)
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "expandRotation"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isExpanded = !isExpanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Solved",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solve.problem.title ?: solve.problem.slug,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = glassColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = solve.problem.platform,
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(difficultyColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = solve.problem.difficulty ?: "?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = difficultyColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = glassColors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotationAngle)
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                val dividerColor = glassColors.textSecondary.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(dividerColor)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Category
                solve.problem.category?.let { categoryIndex ->
                    if (categoryIndex in CATEGORY_NAMES.indices) {
                        DetailRow(
                            icon = Icons.Default.Tag,
                            label = "Category",
                            value = CATEGORY_NAMES[categoryIndex],
                            glassColors = glassColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // XP Awarded
                DetailRow(
                    icon = null,
                    label = "XP Earned",
                    value = "+${solve.xpAwarded} XP",
                    valueColor = Color(0xFFFBBF24),
                    glassColors = glassColors
                )
                
                // Submission details
                solve.submission?.let { submission ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Language
                    submission.language?.let { lang ->
                        DetailRow(
                            icon = Icons.Default.Code,
                            label = "Language",
                            value = lang.replaceFirstChar { it.uppercase() },
                            glassColors = glassColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Time taken
                    submission.timeTaken?.let { seconds ->
                        val minutes = seconds / 60
                        val secs = seconds % 60
                        val timeStr = if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
                        DetailRow(
                            icon = Icons.Default.AccessTime,
                            label = "Time Taken",
                            value = timeStr,
                            glassColors = glassColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Number of tries
                    submission.numberOfTries?.let { tries ->
                        DetailRow(
                            icon = Icons.Default.Refresh,
                            label = "Attempts",
                            value = tries.toString(),
                            glassColors = glassColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Mistake tags
                    if (submission.mistakeTags.isNotEmpty()) {
                        Text(
                            text = "Mistake Tags",
                            fontSize = 12.sp,
                            color = glassColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            submission.mistakeTags.forEach { tag ->
                                TagChip(tag, Color(0xFFEF4444), glassColors)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // AI Analysis
                    submission.aiAnalysis?.let { analysis ->
                        if (analysis.isNotBlank()) {
                            Text(
                                text = "AI Analysis",
                                fontSize = 12.sp,
                                color = glassColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = analysis,
                                fontSize = 13.sp,
                                color = glassColors.textPrimary.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                
                // Highlight data
                solve.highlight?.let { highlight ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (highlight.tags.isNotEmpty()) {
                        Text(
                            text = "Tags",
                            fontSize = 12.sp,
                            color = glassColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            highlight.tags.forEach { tag ->
                                TagChip(tag, Color(0xFF3B82F6), glassColors)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    highlight.note?.let { note ->
                        if (note.isNotBlank()) {
                            Text(
                                text = "Notes",
                                fontSize = 12.sp,
                                color = glassColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note,
                                fontSize = 13.sp,
                                color = glassColors.textPrimary.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector?,
    label: String,
    value: String,
    valueColor: Color? = null,
    glassColors: GlassColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = glassColors.textSecondary
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: glassColors.textPrimary
        )
    }
}

@Composable
private fun TagChip(tag: String, color: Color, glassColors: GlassColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
