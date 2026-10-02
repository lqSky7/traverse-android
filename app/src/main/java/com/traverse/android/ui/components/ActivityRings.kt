package com.traverse.android.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Track colour for both rings.
 *
 * This was `white 0.20f`, chosen so the rings stayed legible on top of the streak card's old
 * lighting-sun background — a dark animated shader whose brightness varied with the streak,
 * where alpha-blending white meant the track lightened where the shader was bright and darkened
 * where it was dim.
 *
 * That shader is gone and the streak card is a flat `#1A1A1A` tile like every other card, so
 * 0.20f is now just a slightly-too-visible ring on a flat surface. 0.14f reads as a track
 * rather than as an unfilled ring.
 */
private val RingTrack = Color.White.copy(alpha = 0.14f)

/**
 * Apple Fitness-style concentric activity rings.
 *
 * Two rings: outer = new solves, inner = completed revisions. Both are drawn against a fixed
 * full-circle track so an untouched day still reads as two rings rather than nothing.
 *
 * Progress is animated rather than snapped — see the two `animateFloatAsState` calls. The rings
 * are the reward for the action that just happened, and a ring that jumps from 0 to 1 in one
 * frame is easy to miss when the user is looking at the submit button.
 */
@Composable
fun ActivityRings(
    solveFraction: Float,
    revisionFraction: Float,
    solveColor: Color,
    revisionColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 76.dp,
    strokeWidth: Dp = 8.dp,
    ringGap: Dp = 3.dp,
    celebratesWhenComplete: Boolean = true
) {
    val animatedSolve by animateFloatAsState(
        targetValue = solveFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "solveRing"
    )
    val animatedRevision by animateFloatAsState(
        targetValue = revisionFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "revisionRing"
    )

    val allClosed = solveFraction >= 1f && revisionFraction >= 1f

    Canvas(modifier = modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val c = center
        val outerR = size.minDimension / 2f - stroke / 2f
        val innerR = outerR - stroke - ringGap.toPx()

        if (celebratesWhenComplete && allClosed) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(solveColor.copy(alpha = 0.28f), Color.Transparent),
                    center = c,
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = c
            )
        }

        // Outer ring: Track first, then fill
        drawRing(outerR, stroke, RingTrack, 0f, c)
        drawRing(outerR, stroke, solveColor, animatedSolve, c)

        // Inner ring: Track first, then fill
        drawRing(innerR, stroke, RingTrack, 0f, c)
        drawRing(innerR, stroke, revisionColor, animatedRevision, c)
    }
}

private fun DrawScope.drawRing(
    radius: Float,
    stroke: Float,
    color: Color,
    fraction: Float,
    c: Offset
) {
    if (fraction <= 0f) {
        drawCircle(
            color = color,
            radius = radius,
            center = c,
            style = Stroke(width = stroke)
        )
        return
    }
    drawArc(
        color = color,
        startAngle = -90f, // 12 o'clock; Compose 0° is 3 o'clock
        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(c.x - radius, c.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}
