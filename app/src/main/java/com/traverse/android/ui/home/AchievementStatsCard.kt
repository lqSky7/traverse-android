package com.traverse.android.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.ui.theme.rememberPalette
import kotlin.math.PI
import kotlin.math.sin

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `AchievementStatsCard`: a hero-only card (unlocked / of N / unlocked)
 * with a breathing radial glow anchored to the bottom edge. No header, no icon and no
 * progress bar — the whole card is a navigation target for the achievements list.
 */
@Composable
fun AchievementStatsCard(
    stats: AchievementStatsData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = rememberPalette().colorAt(3)
    val progress = stats.unlocked.toFloat() / stats.total.coerceAtLeast(1)

    val transition = rememberInfiniteTransition(label = "achievementGlow")
    val glowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "achievementGlowPhase"
    )

    // iOS: 0.15 + (progress * 0.4) * (0.5 + 0.5 * sin(glowPhase))
    val glowFillOpacity = 0.15f + (progress * 0.4f) * (0.5f + 0.5f * sin(glowPhase))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowFillOpacity),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height),
                        radius = 150.dp.toPx()
                    )
                )
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = "${stats.unlocked}",
                fontSize = 72.sp,
                lineHeight = 76.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = "of ${stats.total}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "unlocked",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
