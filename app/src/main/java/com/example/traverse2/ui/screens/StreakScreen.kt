package com.example.traverse2.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.data.api.FriendItem
import com.example.traverse2.ui.components.GlassTopBar
import com.example.traverse2.ui.components.StreakCalendar
import com.example.traverse2.ui.components.StreakDay
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.time.LocalDate

data class FriendStreak(
    val username: String,
    val displayName: String,
    val streak: Int,
    val isActive: Boolean // true if they've solved today
)

@Composable
fun StreakScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
    currentStreak: Int = 0,
    longestStreak: Int = 0,
    totalActiveDays: Int = 0,
    averagePerWeek: Float = 0f,
    friends: List<FriendItem> = emptyList(),
    streakDays: List<StreakDay> = emptyList()
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()

    // Use provided streakDays or generate fallback if empty
    val today = LocalDate.now()
    val displayStreakDays = remember(streakDays) {
        if (streakDays.isNotEmpty()) {
            streakDays
        } else {
            // Fallback: generate from current streak count if no data provided
            (0 until 35).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                StreakDay(
                    date = date,
                    isActive = daysAgo < currentStreak,
                    isToday = daysAgo == 0
                )
            }
        }
    }
    
    // Convert FriendItem to FriendStreak for display
    val friendStreaks = remember(friends) {
        friends.map { friend ->
            FriendStreak(
                username = friend.username,
                displayName = friend.username, // Backend doesn't provide displayName
                streak = friend.currentStreak,
                isActive = friend.currentStreak > 0 // Consider active if they have a streak
            )
        }.sortedByDescending { it.streak }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 100.dp, bottom = 120.dp)
        ) {
            // Current Streak Hero Card
            StreakHeroCard(
                currentStreak = currentStreak,
                hazeState = hazeState,
                glassColors = glassColors
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Streak Stats Row
            StreakStatsCard(
                longestStreak = longestStreak,
                totalActiveDays = totalActiveDays,
                averagePerWeek = averagePerWeek,
                hazeState = hazeState,
                glassColors = glassColors
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Activity Calendar
            StreakCalendar(
                streakDays = displayStreakDays,
                currentStreak = currentStreak,
                hazeState = hazeState,
                glassColors = glassColors
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Friend Streaks Section
            FriendStreaksCard(
                friendStreaks = friendStreaks,
                hazeState = hazeState,
                glassColors = glassColors
            )
        }
        
        // Glass Top Bar
        GlassTopBar(
            title = "Streak",
            hazeState = hazeState,
            glassColors = glassColors,
            onBack = onBack,
            icon = Icons.Default.LocalFireDepartment,
            iconTint = if (glassColors.isDark) Color(0xFFFF6B6B) else Color(0xFFE91E8C)
        )
    }
}

@Composable
private fun StreakHeroCard(
    currentStreak: Int,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    // Animate streak number
    val animatedStreak = remember { Animatable(0f) }
    LaunchedEffect(currentStreak) {
        animatedStreak.animateTo(
            currentStreak.toFloat(),
            tween(1000, easing = FastOutSlowInEasing)
        )
    }
    
    val streakColor = when {
        currentStreak >= 30 -> Color(0xFFFFD700) // Gold
        currentStreak >= 14 -> Color(0xFFFF6B6B) // Red-orange
        currentStreak >= 7 -> Color(0xFFE91E8C) // Pink
        else -> if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6) // Blue
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
                if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x50FFFFFF)
            )
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fire icon with glow effect
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(streakColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = streakColor,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Streak number
            Text(
                text = "${animatedStreak.value.toInt()}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = streakColor
            )
            
            Text(
                text = "day streak",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Motivational message
            val message = when {
                currentStreak == 0 -> "Start your streak today!"
                currentStreak < 3 -> "Great start! Keep going!"
                currentStreak < 7 -> "Building momentum!"
                currentStreak < 14 -> "One week strong!"
                currentStreak < 30 -> "You're unstoppable!"
                else -> "Legendary coder!"
            }
            
            Text(
                text = message,
                fontSize = 16.sp,
                color = glassColors.textSecondary
            )
        }
    }
}

@Composable
private fun StreakStatsCard(
    longestStreak: Int,
    totalActiveDays: Int,
    averagePerWeek: Float,
    hazeState: HazeState,
    glassColors: GlassColors
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
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.EmojiEvents,
                value = "$longestStreak",
                label = "Longest",
                color = Color(0xFFFFD700),
                glassColors = glassColors
            )
            
            StatItem(
                icon = Icons.Default.Schedule,
                value = "$totalActiveDays",
                label = "Active Days",
                color = Color(0xFF22C55E),
                glassColors = glassColors
            )
            
            StatItem(
                icon = Icons.Default.TrendingUp,
                value = String.format("%.1f", averagePerWeek),
                label = "Avg/Week",
                color = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 24.sp,
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
private fun FriendStreaksCard(
    friendStreaks: List<FriendStreak>,
    hazeState: HazeState,
    glassColors: GlassColors
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
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Friend Streaks",
                    tint = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Friend Streaks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Friend list
            friendStreaks.sortedByDescending { it.streak }.forEachIndexed { index, friend ->
                FriendStreakItem(
                    friend = friend,
                    rank = index + 1,
                    glassColors = glassColors
                )
                
                if (index < friendStreaks.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FriendStreakItem(
    friend: FriendStreak,
    rank: Int,
    glassColors: GlassColors
) {
    val bgColor = if (glassColors.isDark) Color(0x18FFFFFF) else Color(0x15000000)
    
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> glassColors.textSecondary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (rank <= 3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rank $rank",
                    tint = rankColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary
            )
            Text(
                text = "@${friend.username}",
                fontSize = 12.sp,
                color = glassColors.textSecondary
            )
        }
        
        // Streak count
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (friend.isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = if (friend.streak > 0) Color(0xFFFF6B6B) else glassColors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${friend.streak}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (friend.streak > 0) glassColors.textPrimary else glassColors.textSecondary
            )
        }
    }
}
