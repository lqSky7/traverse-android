package com.traverse.android.ui.home

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val PI_F = 3.1415927f

/**
 * 1:1 port of the iOS `LightingSunBackground` / `AnimatableLightingSun` pair, which powers the
 * rainbow light dispersion behind the home screen's streak card.
 *
 * On iOS this applies the Metal `lightingSimulation` shader (from radiofun's LightingSim) as a
 * `layerEffect` over `Color.black`. Because the sampled layer is pure black, the shader result is
 * completely procedural — there is no layer content to sample — so we reproduce the exact same
 * maths on the CPU and blit the result as a scaled bitmap. That keeps the look identical on every
 * API level without depending on AGSL/RuntimeShader (API 33+ only).
 *
 * Uniform mapping, mirroring the iOS call site:
 *   size      = (5.0, cardHeight / 1.2)
 *   intensity = 0.3 + p * 2.2
 *   disperse  = 0.15 + p * 0.60
 *   rotation  = -PI / 2
 *   radius    = 10 + p * 40
 * where `p = clamp(streak, 0, 15) / 15` is spring-animated from 0 on appear.
 */
@Composable
fun LightingSunBackground(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val target = streak.coerceIn(0, 15).toFloat() / 15f

    // iOS: .spring(response: 1.1, dampingFraction: 0.72)
    LaunchedEffect(streak) {
        progress.animateTo(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 33f)
        )
    }

    val animated = progress.value
    // Quantise so we don't rebuild the bitmap for imperceptible sub-pixel changes.
    val quantised = (animated * 140f).roundToInt() / 140f

    val intensity = 0.3f + quantised * 2.2f
    val disperse = 0.15f + quantised * 0.60f
    val radius = 10f + quantised * 40f

    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val density = LocalDensity.current.density
        val widthDp = if (maxWidth.value.isFinite()) maxWidth.value else 1f
        val heightDp = if (maxHeight.value.isFinite()) maxHeight.value else 110f
        val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)

        val bitmap = remember(widthPx, heightPx, density, quantised) {
            renderLightingSun(
                widthPx = widthPx,
                heightPx = heightPx,
                density = density,
                intensity = intensity,
                disperse = disperse,
                rotation = -PI_F / 2f,
                radius = radius
            )
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.Low
        )
    }
}

/**
 * Renders the `lightingSimulation` light field into a bitmap.
 *
 * Everything is computed in dp ("points", matching SwiftUI's coordinate space) so the beam
 * proportions and distance falloff line up with iOS regardless of screen density.
 */
private fun renderLightingSun(
    widthPx: Int,
    heightPx: Int,
    density: Float,
    intensity: Float,
    disperse: Float,
    rotation: Float,
    radius: Float
): Bitmap {
    // The effect is inherently soft, so we can render at a fraction of the card resolution and
    // let bilinear upscaling smooth it out. This keeps the per-frame cost negligible.
    val totalPx = widthPx.toLong() * heightPx.toLong()
    val maxPixels = 14000f
    val scale = if (totalPx > maxPixels) sqrt(maxPixels / totalPx.toFloat()) else 1f
    val w = (widthPx * scale).roundToInt().coerceIn(8, 640)
    val h = (heightPx * scale).roundToInt().coerceIn(8, 640)

    val cardW = widthPx / density
    val cardH = heightPx / density

    // iOS: targetY = cardHeight / 1.2, size = (5.0, targetY)
    val sizeX = 5f
    val sizeY = cardH / 1.2f

    val sourceX = 0.5f * sizeX
    val sourceY = 0.6f * sizeY

    val angle = (PI_F / 2f) + rotation
    val dirX = cos(angle)
    val dirY = sin(angle)
    val beamLen = hypot(sizeX, sizeY)
    val endX = sourceX + dirX * beamLen
    val endY = sourceY + dirY * beamLen

    val baX = endX - sourceX
    val baY = endY - sourceY
    val baLen2 = (baX * baX + baY * baY).coerceAtLeast(1e-6f)

    val w0 = radius
    val spreadFactor = 2f * disperse
    val outerSpreadFactor = spreadFactor * 8f
    val clampedDisperse = min(disperse, 0.78f) / 0.78f
    val perpX = -dirY
    val perpY = dirX

    val stepX = cardW / w
    val stepY = cardH / h

    val pixels = IntArray(w * h)

    for (j in 0 until h) {
        val posY = (j + 0.5f) * stepY
        val rowOffset = j * w
        for (i in 0 until w) {
            val posX = (i + 0.5f) * stepX

            // Signed distance to the beam axis + parametric position along the beam.
            val paX = posX - sourceX
            val paY = posY - sourceY
            val t = ((paX * baX + paY * baY) / baLen2).coerceIn(0f, 1f)
            val distToAxis = hypot(paX - baX * t, paY - baY * t)

            val distAlongAxis = t * beamLen
            val startSmoothing = smoothstep(0f, 0.2f, t)
            val currentWidth = w0 + distAlongAxis * spreadFactor * startSmoothing
            val offsetAmt = 160f * clampedDisperse * t

            // Chromatic aberration: R and B sample perpendicular to the beam, G stays put.
            val distR = segmentDistance(
                posX + perpX * offsetAmt, posY + perpY * offsetAmt,
                sourceX, sourceY, endX, endY
            )
            val distB = segmentDistance(
                posX - perpX * offsetAmt, posY - perpY * offsetAmt,
                sourceX, sourceY, endX, endY
            )

            val beamR = (1f - smoothstep(0f, currentWidth, distR)).pow(1.2f)
            val beamG = (1f - smoothstep(0f, currentWidth, distToAxis)).pow(1.2f)
            val beamB = (1f - smoothstep(0f, currentWidth, distB)).pow(1.2f)

            val widthRatio = w0 / currentWidth
            val flux = widthRatio * widthRatio
            val distFromSource = hypot(posX - sourceX, posY - sourceY)
            val falloff = 1f / (1f + distFromSource * 0.005f)

            val outerWidth = w0 + distAlongAxis * outerSpreadFactor * startSmoothing
            val outerBeam = (1f - smoothstep(0f, outerWidth, distToAxis)).pow(3.5f)
            val outerIntensity = 0.05f * intensity * falloff

            val mainScale = intensity * flux * falloff
            val outer = outerBeam * outerIntensity

            // The iOS layer effect samples Color.black, so original.rgb == 0 and the final
            // colour is just the combined light (tone mapped).
            val r = toneMap(beamR * mainScale + outer)
            val g = toneMap(beamG * mainScale + outer)
            val b = toneMap(beamB * mainScale + outer)

            pixels[rowOffset + i] = packArgb(r, g, b)
        }
    }

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    return bitmap
}

private fun segmentDistance(
    px: Float, py: Float,
    ax: Float, ay: Float,
    bx: Float, by: Float
): Float {
    val pax = px - ax
    val pay = py - ay
    val bax = bx - ax
    val bay = by - ay
    val len2 = bax * bax + bay * bay
    val t = if (len2 <= 0f) 0f else ((pax * bax + pay * bay) / len2).coerceIn(0f, 1f)
    return hypot(pax - bax * t, pay - bay * t)
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge0 == edge1) return if (x < edge0) 0f else 1f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** ACES-style filmic tone map from the original shader. */
private fun toneMap(c: Float): Float {
    val v = (c * (2.51f * c + 0.03f)) / (c * (2.43f * c + 0.59f) + 0.14f)
    return v.coerceIn(0f, 1f)
}

private fun packArgb(r: Float, g: Float, b: Float): Int {
    val ri = (r * 255f).roundToInt().coerceIn(0, 255)
    val gi = (g * 255f).roundToInt().coerceIn(0, 255)
    val bi = (b * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
}
