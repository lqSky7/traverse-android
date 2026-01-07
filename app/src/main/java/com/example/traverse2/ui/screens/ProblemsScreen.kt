package com.example.traverse2.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.components.GlassTopBar
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

@Composable
fun ProblemsScreen(
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    
    // Mock data - would come from API
    val problemsCompleted = 42
    val totalProblems = 100
    val percentage = if (totalProblems > 0) (problemsCompleted.toFloat() / totalProblems * 100).toInt() else 0
    
    val allProblems = listOf(
        ProblemData("Two Sum", "LeetCode", "Easy", true),
        ProblemData("Valid Parentheses", "LeetCode", "Easy", true),
        ProblemData("Merge Intervals", "LeetCode", "Medium", true),
        ProblemData("LRU Cache", "LeetCode", "Hard", true),
        ProblemData("Three Sum", "LeetCode", "Medium", false),
        ProblemData("Binary Search", "LeetCode", "Easy", false),
        ProblemData("Graph Clone", "LeetCode", "Medium", false)
    )
    
    val solvedProblems = allProblems.filter { it.solved }
    val unsolvedProblems = allProblems.filter { !it.solved }
    
    val progressColor = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
    val trackColor = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x30E91E8C)
    val successColor = Color(0xFF22C55E)
    val warningColor = Color(0xFFFBBF24)
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                    // Animated circular progress
                    val progress = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
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
                    
                    // Solved count
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
                    
                    // Unsolved count
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Unsolved",
                                tint = warningColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${totalProblems - problemsCompleted}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = warningColor
                            )
                        }
                        Text(
                            text = "Remaining",
                            fontSize = 13.sp,
                            color = glassColors.textSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Solved Problems Section
            if (solvedProblems.isNotEmpty()) {
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
                                text = "(${solvedProblems.size})",
                                fontSize = 14.sp,
                                color = glassColors.textSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        solvedProblems.forEachIndexed { index, problem ->
                            AnimatedProblemItem(problem, glassColors, index * 80)
                            if (index < solvedProblems.size - 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Unsolved Problems Section
            if (unsolvedProblems.isNotEmpty()) {
                GlassCard(hazeState = hazeState, glassColors = glassColors) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(warningColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Unsolved Problems",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = glassColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${unsolvedProblems.size})",
                                fontSize = 14.sp,
                                color = glassColors.textSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        unsolvedProblems.forEachIndexed { index, problem ->
                            AnimatedProblemItem(problem, glassColors, (solvedProblems.size + index) * 80)
                            if (index < unsolvedProblems.size - 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // Fixed Glass Top Bar
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
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
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
private fun AnimatedProblemItem(problem: ProblemData, glassColors: GlassColors, delayMs: Int) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "problemItemAlpha"
    )
    
    ProblemListItem(
        problem = problem,
        glassColors = glassColors,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun ProblemListItem(
    problem: ProblemData,
    glassColors: GlassColors,
    modifier: Modifier = Modifier
) {
    val difficultyColor = when (problem.difficulty.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    val bgColor = if (problem.solved) {
        if (glassColors.isDark) Color(0x18FFFFFF) else Color(0x15000000)
    } else {
        if (glassColors.isDark) Color(0x0CFFFFFF) else Color(0x0A000000)
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (problem.solved) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (problem.solved) "Solved" else "Not solved",
                tint = if (problem.solved) Color(0xFF22C55E) else glassColors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = problem.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (problem.solved) glassColors.textPrimary else glassColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = problem.platform,
                    fontSize = 12.sp,
                    color = glassColors.textSecondary
                )
            }
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(difficultyColor.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = problem.difficulty,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = difficultyColor
            )
        }
    }
}

private data class ProblemData(
    val name: String,
    val platform: String,
    val difficulty: String,
    val solved: Boolean
)
