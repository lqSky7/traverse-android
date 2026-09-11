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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.revisions.AnalyticsInfoSheet
import com.traverse.android.ui.theme.Peach

private val ScoreCardBackground = Color.Black
private val ScoreAccent = Color(0xFFB8D4E3)

private const val REVISION_SCORE_EXPLANATION =
    "Tracks your overall revision consistency and memory retention health over the past 7 days.\n\n" +
        "\u2022 Memory Retention: Measures how effectively your review habit reinforces learned DSA " +
        "concepts to maintain strong long-term recall.\n\n" +
        "\u2022 Outcome-Independent: Focuses purely on engagement and recall effort \u2014 it is not " +
        "penalized when you struggle on difficult problems."

/**
 * Revision health score card — 1:1 port of the iOS `RevisionScoreCard`.
 *
 * A black card showing the 7-day revision score with a soft animated "thinking orb"
 * in the top-right corner. Tapping it opens the explanation sheet.
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
            .height(120.dp)
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
                .offset(x = 28.dp, y = (-28).dp)
                .size(120.dp)
        )

        Text(
            text = "$score",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (showExplanation) {
        AnalyticsInfoSheet(
            title = "Revision Score",
            description = REVISION_SCORE_EXPLANATION,
            onDismiss = { showExplanation = false }
        )
    }
}

/**
 * Soft ambient glow whose scale and opacity scale with [score], mirroring the
 * iOS "Thinking Orb" shader effect without needing a GPU shader.
 */
@Composable
private fun ThinkingOrbScoreView(
    score: Int,
    modifier: Modifier = Modifier
) {
    val normalized = score.coerceIn(0, 100) / 100f

    val transition = rememberInfiniteTransition(label = "revisionOrb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "revisionOrbPhase"
    )

    val orbScale = 0.55f + 0.35f * normalized
    val orbAlpha = 0.40f + 0.55f * normalized

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Primary ambient glow body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = orbAlpha
                    scaleX = orbScale
                    scaleY = orbScale
                    rotationZ = phase * 0.7f
                }
                .background(
                    brush = Brush.radialGradient(colors = listOf(Peach, Color.Transparent)),
                    shape = CircleShape
                )
        )

        // Secondary core filament
        Box(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .graphicsLayer {
                    alpha = orbAlpha
                    scaleX = orbScale
                    scaleY = orbScale * 0.62f
                    rotationZ = -phase * 0.7f
                }
                .background(
                    brush = Brush.radialGradient(colors = listOf(ScoreAccent, Color.Transparent)),
                    shape = CircleShape
                )
        )
    }
}
