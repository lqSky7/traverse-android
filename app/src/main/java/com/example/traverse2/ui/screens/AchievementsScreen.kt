package com.example.traverse2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.components.GlassTopBar
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun AchievementsScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
    achievements: List<AchievementData> = emptyList()
) {
    val glassColors = TraverseTheme.glassColors
    
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        if (glassColors.isDark) Color(0xFF000000) else Color(0xFFf5f5f7),
                        if (glassColors.isDark) Color(0xFF0A0A0A) else Color(0xFFe8e8ed)
                    )
                )
            )
    ) {
        // Top bar
        GlassTopBar(
            hazeState = hazeState,
            glassColors = glassColors,
            onBack = onBack,
            title = "Achievements"
        )
        
        // Main content - Use LazyVerticalGrid for everything (it handles scrolling)
        if (achievements.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Stats card as header
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Column {
                        StatsCard(unlockedCount, totalCount, glassColors, hazeState)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "All Achievements",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = glassColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Achievement items
                itemsIndexed(achievements) { index, achievement ->
                    AchievementCardDetailed(achievement, glassColors, hazeState)
                }
                
                // Bottom spacer
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    StatsCard(unlockedCount, totalCount, glassColors, hazeState)
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "No achievements yet",
                        color = glassColors.textSecondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start solving problems to unlock achievements!",
                        color = glassColors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    unlockedCount: Int,
    totalCount: Int,
    glassColors: GlassColors,
    hazeState: HazeState
) {
    val percentage = if (totalCount > 0) (unlockedCount * 100 / totalCount) else 0
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFFE91E8C),
                    blurRadius = 20.dp,
                    tint = HazeTint(Color(0x40E91E8C)),
                    noiseFactor = 0.02f
                )
            )
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0x80E91E8C), Color(0x80A855F7))
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progress",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$unlockedCount / $totalCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Progress circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$percentage%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun AchievementCardDetailed(
    achievement: AchievementData,
    glassColors: GlassColors,
    hazeState: HazeState
) {
    if (achievement.name.isEmpty()) {
        return
    }

    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    val unlockedColor = Color(0xFF22C55E)
    val lockedColor = glassColors.textSecondary.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp) // Fixed height for uniform card size
            .clip(RoundedCornerShape(16.dp))
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
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon/Status
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) unlockedColor.copy(alpha = 0.2f)
                        else lockedColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = achievement.name,
                    tint = if (achievement.unlocked) unlockedColor else lockedColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name with fixed height to prevent layout shifts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status
            Text(
                text = if (achievement.unlocked) "Unlocked" else "Locked",
                fontSize = 10.sp,
                color = if (achievement.unlocked) unlockedColor else glassColors.textSecondary
            )
        }
    }
}
