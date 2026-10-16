package com.traverse.android.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The static glass surface — for anything that cannot sample a backdrop.
 *
 * The real thing is [glurBackdrop] and [backdropGlass], which blur the content
 * behind the surface using the vendored Kyant backdrop library. Those need a
 * [GlassBackdrop] capture of what sits behind, and a surface *inside* the
 * scrolling feed cannot have one: it would be sampling a capture that contains
 * itself, so the blurs would compound frame over frame.
 *
 * The glance cards and action chips live in the feed, so they use this
 * instead: a translucent fill, a specular catch along the top edge, and a
 * hairline border. Same language as the backdrop glass, no capture required.
 */

/** Liquid-glass surface: translucent fill, top highlight, hairline border. */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    tint: Color = Color.White,
    tintAlpha: Float = 0.16f,
    highlightAlpha: Float = 0.22f,
    borderAlpha: Float = 0.18f,
    borderWidth: Dp = 0.7.dp
): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = tintAlpha))
    .drawBehind {
        // The specular catch along the top edge — the thing that reads as glass
        // rather than as a flat translucent rectangle.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = highlightAlpha),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.55f
            )
        )
    }
    .border(borderWidth, Color.White.copy(alpha = borderAlpha), shape)

/**
 * The progressive header backdrop — this project's "glur" for Android.
 *
 * Content scrolling under a header dissolves into the app's black background
 * through a vertical gradient: opaque at the top, clear at the bottom. This is
 * the same treatment the iOS build settled on (a gradient scrim plus frost),
 * and unlike a backdrop blur it is deterministic: always visible, never cut
 * off, and it can't end in a hard line.
 *
 * @param topAlpha opacity of the scrim at the very top edge.
 * @param fadeAt fraction of the height where the scrim has fully faded out.
 */
@Composable
fun ProgressiveHeaderScrim(
    modifier: Modifier = Modifier,
    topAlpha: Float = 0.92f,
    fadeAt: Float = 1f
) {
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = topAlpha),
                        (fadeAt * 0.45f).coerceIn(0f, 1f) to Color.Black.copy(alpha = topAlpha * 0.72f),
                        (fadeAt * 0.78f).coerceIn(0f, 1f) to Color.Black.copy(alpha = topAlpha * 0.32f),
                        fadeAt.coerceIn(0f, 1f) to Color.Transparent
                    )
                )
            )
        }
    )
}
