package com.example.traverse2.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun HomeContent(
    hazeState: HazeState,
    onLogout: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    
    // Mock data - replace with actual API calls
    val currentStreak = 7
    val totalSolves = 42
    val totalXp = 680
    val problemsCompleted = 42
    val totalProblems = 100
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp, bottom = 120.dp)
    ) {
        // Top Bar with greeting and logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stay curious, keep coding!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
            
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = glassColors.textSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Streak Card - Pink/Purple gradient
        StreakCard(
            streak = currentStreak,
            hazeState = hazeState
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Your Work Card - Glassy with stats
        YourWorkCard(
            totalSolves = totalSolves,
            totalXp = totalXp,
            streak = currentStreak,
            hazeState = hazeState,
            glassColors = glassColors
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Achievements Card with circular progress
        AchievementsCard(
            completed = problemsCompleted,
            total = totalProblems,
            hazeState = hazeState,
            glassColors = glassColors
        )
    }
}

@Composable
private fun StreakCard(
    streak: Int,
    hazeState: HazeState
) {
    val streakComment = when {
        streak == 0 -> "Start your streak today!"
        streak < 3 -> "Good start! Keep it going!"
        streak < 7 -> "You're building momentum!"
        streak < 14 -> "One week strong! Amazing!"
        streak < 30 -> "Unstoppable! Keep pushing!"
        else -> "Legendary coder! $streak days!"
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFFE91E8C),
                    blurRadius = 40.dp,
                    tint = HazeTint(Color(0x40E91E8C)),
                    noiseFactor = 0.02f
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x80E91E8C),
                        Color(0x80A855F7)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$streak",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "day streak",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = streakComment,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun YourWorkCard(
    totalSolves: Int,
    totalXp: Int,
    streak: Int,
    hazeState: HazeState,
    glassColors: com.example.traverse2.ui.theme.GlassColors
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
            .background(
                if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF)
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Your Work",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    icon = Icons.Default.CheckCircle,
                    value = totalSolves.toString(),
                    label = "Total Solves",
                    glassColors = glassColors
                )
                
                StatColumn(
                    icon = Icons.Default.Star,
                    value = totalXp.toString(),
                    label = "Total XP",
                    glassColors = glassColors
                )
                
                StatColumn(
                    icon = Icons.Default.LocalFireDepartment,
                    value = streak.toString(),
                    label = "Streak",
                    glassColors = glassColors
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    glassColors: com.example.traverse2.ui.theme.GlassColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = glassColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = glassColors.textPrimary
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = glassColors.textSecondary
        )
    }
}

@Composable
private fun AchievementsCard(
    completed: Int,
    total: Int,
    hazeState: HazeState,
    glassColors: com.example.traverse2.ui.theme.GlassColors
) {
    val percentage = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    val progressColor = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
    val trackColor = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x30E91E8C)
    
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
            .background(
                if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress ring
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Progress arc
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Text(
                    text = "$percentage%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column {
                Text(
                    text = "Problem Completion",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$completed of $total problems solved",
                    fontSize = 14.sp,
                    color = glassColors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (percentage >= 50) "Great progress!" else "Keep going!",
                    fontSize = 12.sp,
                    color = if (glassColors.isDark) Color.White.copy(alpha = 0.7f) else Color(0xFFE91E8C)
                )
            }
        }
    }
}
