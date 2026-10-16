package com.traverse.android.ui.components.glass

import android.os.Build
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
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Android counterpart of the iOS glass language.
 *
 * The look is the same as the ClearPDF / Kyant liquid glass: a translucent
 * surface, a specular highlight along the top edge, and a hairline border.
 *
 * A true backdrop blur (sampling and blurring whatever sits *behind* the
 * surface) needs the `com.kyant.backdrop` library that ClearPDF vendors — that
 * library requires Kotlin 2.3 + AGP 9 + the `-Xcontext-parameters` compiler
 * flag, which this project's toolchain (Kotlin 2.0.21 / AGP 8.13) can't
 * compile. Everything here therefore reproduces the appearance natively: no
 * new dependency, works on the current toolchain.
 */

/** Liquid-glass surface: translucent fill, top highlight, hairline border. */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    tint: Color = Color.White,
    tintAlpha: Float = 0.10f,
    highlightAlpha: Float = 0.18f,
    borderAlpha: Float = 0.14f,
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

/**
 * A real blur, where the platform can do it (API 31+). Used for full-surface
 * glass panels that sit over media rather than over the app's own list
 * content; the header uses [ProgressiveHeaderScrim] instead.
 */
fun Modifier.platformBlur(radius: Dp): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(radius.toPx(), radius.toPx(), TileMode.Clamp)
        }
    } else {
        Modifier
    }
)
