package com.traverse.android.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.theme.rememberPalette
import kotlin.math.PI
import kotlin.math.sin

private val ScoreCardBackground = Color.Black

private const val RAD_TO_DEG = 57.29578f

/**
 * 1:1 port of the iOS `RevisionScoreCard`.
 *
 * A pure-black card showing the 7-day revision score with the animated "Thinking Orb"
 * pushed into the top-right corner. Tapping anywhere opens the explanation sheet.
 */
@Composable
fun RevisionScoreCard(
    score: Int,
    modifier: Modifier = Modifier
) {
    var showExplanation by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ScoreCardBackground)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { showExplanation = true }
    ) {
        ThinkingOrbScoreView(
            score = score,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
        )

        Text(
            text = "$score",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (showExplanation) {
        RevisionScoreExplanationSheet(onDismiss = { showExplanation = false })
    }
}

/**
 * 1:1 port of the iOS `ThinkingOrbScoreView`: three additive, blurred layers whose
 * rotation speed, scale and opacity all scale with [score]. Colours come from the active
 * palette — `color(at: 0)` for the ambient body and `color(at: 1)` for the core layer.
 */
@Composable
private fun ThinkingOrbScoreView(
    score: Int,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val orbPrimary = palette.colorAt(0)
    val orbSecondary = palette.colorAt(1)

    val normalized = score.coerceIn(0, 100) / 100f

    // iOS drives the phase from the render clock: speed = 0.6 + normalized * 0.8.
    val speed = 0.6f + normalized * 0.8f
    val durationMs = ((2.0 * PI / speed) * 1000.0).toInt().coerceAtLeast(1)

    val transition = rememberInfiniteTransition(label = "revisionOrb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "revisionOrbPhase"
    )
    val progress = sin(phase)

    val orbScale = 0.55f + 0.35f * normalized
    val orbAlpha = 0.40f + 0.55f * normalized

    Box(
        modifier = modifier
            .size(110.dp)
            .graphicsLayer {
                scaleX = orbScale
                scaleY = orbScale
                alpha = orbAlpha
                blendMode = BlendMode.Screen
            },
        contentAlignment = Alignment.Center
    ) {
        // Primary ambient glow body
        Box(
            modifier = Modifier
                .size(110.dp)
                .graphicsLayer { rotationZ = progress * 0.7f * RAD_TO_DEG }
                .blur(24.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(orbPrimary, orbSecondary),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(120.dp)
                )
        )

        // Secondary core layer
        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 42.dp)
                .graphicsLayer { rotationZ = -progress * 0.7f * RAD_TO_DEG }
                .blur(18.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    color = lerp(orbSecondary, Color.White, 0.35f),
                    shape = RoundedCornerShape(120.dp)
                )
        )

        // Bright white filament highlight
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 40.dp)
                .offset(y = (-14).dp)
                .graphicsLayer { rotationZ = progress * 0.35f * RAD_TO_DEG }
                .blur(24.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    color = Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(120.dp)
                )
        )
    }
}
