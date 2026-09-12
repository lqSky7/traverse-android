package com.traverse.android.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.ui.theme.rememberPalette

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `AchievementStatsCard`: a hero-only card (unlocked / of N / unlocked)
 * with a radial glow anchored to the bottom edge. No header, no icon and no progress bar — the
 * whole card is a navigation target for the achievements list.
 */
@Composable
fun AchievementStatsCard(
    stats: AchievementStatsData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = rememberPalette().colorAt(3)
    val progress = stats.unlocked.toFloat() / stats.total.coerceAtLeast(1)

    // iOS drives the glow with `.repeatForever(autoreverses: true)` and modulates it through
    // `sin(glowPhase)`. Here the card sits inside the scrolling home feed, where a per-frame
    // animation would recompose the entire feed on every frame for a purely decorative effect, so
    // the glow is pinned to the mid-point of the iOS range (its `animationFactor` averages 0.5).
    // The resting look is identical to iOS; only the breathing motion is dropped.
    val glowFillOpacity = 0.15f + (progress * 0.4f) * 0.5f

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
                // iOS is `.font(.system(size: 72, weight: .bold))` with no line height of its own.
                // Compose's `lineHeight` is the distance between baselines, so a value *below* the
                // font's natural line height (~84sp for 72sp Roboto) makes the glyph overflow its
                // own line box — which is what dragged the number off the card's optical centre.
                // Leaving it unset restores the natural metrics iOS uses.
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "of ${stats.total}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "unlocked",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
