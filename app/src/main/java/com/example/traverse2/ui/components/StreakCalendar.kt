package com.example.traverse2.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.GlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StreakDay(
    val date: LocalDate,
    val isActive: Boolean,  // true = solved problem that day
    val isToday: Boolean = false
)

@Composable
fun StreakCalendar(
    streakDays: List<StreakDay>,
    currentStreak: Int,
    hazeState: HazeState,
    glassColors: GlassColors,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)

    // Get the current month's calendar view
    val today = LocalDate.now()

    // Find the first day of the current month
    val firstDayOfMonth = today.withDayOfMonth(1)

    // Get the day of week offset (Monday = 0, Sunday = 6)
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1) // Monday-based (0-6)

    // Get the number of days in the current month
    val daysInMonth = today.lengthOfMonth()

    // Calculate total cells needed (offset + days in month, rounded up to complete weeks)
    val totalCells = firstDayOffset + daysInMonth
    val numWeeks = (totalCells + 6) / 7 // Ceiling division

    // Generate calendar data - properly aligned with day headers
    val weeks = (0 until numWeeks).map { weekIndex ->
        (0 until 7).map { dayIndex ->
            val cellIndex = weekIndex * 7 + dayIndex
            val dayOfMonth = cellIndex - firstDayOffset + 1

            if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                // Empty cell (before first day or after last day of month)
                null
            } else {
                val date = firstDayOfMonth.plusDays((dayOfMonth - 1).toLong())
                val streakDay = streakDays.find { it.date == date }
                streakDay ?: StreakDay(date, isActive = false, isToday = date == today)
            }
        }
    }
    
    Box(
        modifier = modifier
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
            .padding(20.dp)
    ) {
        Column {
            // Header with month name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Calendar",
                        tint = if (glassColors.isDark) Color(0xFFFF6B6B) else Color(0xFFE91E8C),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary
                    )
                }

                Text(
                    text = "$currentStreak day streak",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (currentStreak > 0) {
                        if (glassColors.isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
                    } else {
                        glassColors.textSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = glassColors.textSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            weeks.forEachIndexed { weekIndex, week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEachIndexed { dayIndex, day ->
                        CalendarDayCell(
                            day = day,
                            glassColors = glassColors,
                            delayMs = (weekIndex * 7 + dayIndex) * 20,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (weekIndex < weeks.size - 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = if (glassColors.isDark) Color(0xFF22C55E) else Color(0xFF16A34A),
                    label = "Active",
                    glassColors = glassColors
                )
                Spacer(modifier = Modifier.size(20.dp))
                LegendItem(
                    color = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x20000000),
                    label = "Missed",
                    glassColors = glassColors
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: StreakDay?,
    glassColors: GlassColors,
    delayMs: Int,
    modifier: Modifier = Modifier
) {
    // Empty cell for padding (before first day or after last day of month)
    if (day == null) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(3.dp)
        )
        return
    }

    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        alpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    val activeColor = if (glassColors.isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
    val inactiveColor = if (glassColors.isDark) Color(0x25FFFFFF) else Color(0x15000000)
    val todayBorderColor = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)

    val bgColor = when {
        day.isActive -> activeColor
        else -> inactiveColor
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (day.isToday && day.isActive) 1.1f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .scale(scale.value * pulseScale)
            .alpha(alpha.value)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (day.isActive && day.isToday) {
                    Brush.linearGradient(
                        colors = listOf(activeColor, Color(0xFF10B981))
                    )
                } else {
                    Brush.linearGradient(colors = listOf(bgColor, bgColor))
                }
            )
            .then(
                if (day.isToday && !day.isActive) {
                    Modifier.background(
                        color = todayBorderColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.isActive) {
            // Small fire icon for active days
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Active",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    glassColors: GlassColors
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = glassColors.textSecondary
        )
    }
}
