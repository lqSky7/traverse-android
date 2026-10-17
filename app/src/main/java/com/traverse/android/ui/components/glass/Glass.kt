package com.traverse.android.ui.components.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.traverse.android.ui.components.glass.effects.blur
import com.traverse.android.ui.components.glass.effects.lens
import com.traverse.android.ui.components.glass.effects.vibrancy
import com.traverse.android.ui.components.glass.highlight.Highlight
import com.traverse.android.ui.components.glass.shadow.InnerShadow
import com.traverse.android.ui.components.glass.shadow.Shadow

/**
 * Liquid glass, the real thing.
 *
 * Every surface in the app is drawn by the vendored Kyant backdrop library: it
 * samples what is *behind* it, runs the same effect stack ClearPDF uses
 * (vibrancy → frost → lens), and composites a specular rim, an inner shadow and
 * a thin surface wash on top. There is no static "translucent rectangle plus a
 * border" anywhere — that approximation was invisible over this app's black
 * background, which is exactly why the UI read as having no glass at all.
 *
 * ### The glass-on-glass problem, and how it is avoided
 *
 * A backdrop is a *capture* of a layer. A surface that lives inside the layer
 * it samples would be feeding its own output back in, and the blur would
 * compound frame after frame. So the rule is structural, not cosmetic:
 *
 * - The pinned header and its glance cards sit *outside* the capture of the
 *   scrolling feed, so they sample the live feed — content really does refract
 *   through them as it scrolls underneath.
 * - The action chips live *inside* the scrolling feed (they are its first
 *   item, so they scroll away under the header). They sample
 *   [rememberSolidGlassBackdrop] instead: the same effect stack over the app's
 *   own background. That is not a fallback for looks — behind a chip there is
 *   nothing but the background anyway, so the pixels are identical while the
 *   feedback loop is impossible.
 *
 * ### When glass is not available
 *
 * `RenderEffect` blur needs API 31 and the lens shader needs API 33, and the
 * user can turn glass off entirely with Settings → App → Performance mode.
 * In all of those cases [liquidGlass] draws an *opaque* surface instead —
 * [glassOpaqueColor], the wash composited over the background, which is exactly
 * what the glass looks like over a flat backdrop. Opaque, not translucent: a
 * translucent fill with nothing blurred behind it is what made the old UI read
 * as having no material at all.
 */

/** The backdrop a nested glass surface should sample, if its screen provided one. */
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** True where the platform can run the blur pass. */
val isGlassBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** True where the platform can run the refraction (lens) shader. */
val isGlassLensSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** The shared design values, so every glass surface in the app is one material. */
object GlassDefaults {

    /** Glance cards and action chips. */
    val CardShape: Shape = RoundedCornerShape(18.dp)

    /** Pill-shaped surfaces: the tab bar, its selected pill, the refresh capsule. */
    val Capsule: Shape = RoundedCornerShape(50)

    /**
     * The wash over the refracted backdrop. It is what keeps a surface legible
     * over this app's black background — pure glass over black is black. Kept
     * light on purpose: a heavy dark plate was rejected.
     */
    val Surface: Color = Color.White.copy(alpha = 0.14f)

    /** Frost for small surfaces; the lens does the rest of the work. */
    val BlurRadius: Dp = 8.dp

    /** How far in from the edge the refraction band reaches. */
    val RefractionHeight: Dp = 12.dp

    /** How far the edge bends the sampled content. */
    val RefractionAmount: Dp = 24.dp
}

// MARK: - Backdrops

/**
 * A backdrop that is just a colour.
 *
 * Used where a surface has nothing but the app background behind it — it runs
 * the identical effect stack, it simply has no live content to sample. This is
 * the same idea as ClearPDF's flat-wallpaper path, and it is the reason the
 * chips and the tab bar can be real glass without a feedback loop.
 */
@Immutable
private class SolidGlassBackdrop(
    private val color: Color
) : GlassBackdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        drawRect(color)
    }
}

/** A backdrop that is a flat [color]; see [SolidGlassBackdrop]. */
@Composable
fun rememberSolidGlassBackdrop(color: Color): GlassBackdrop =
    remember(color) { SolidGlassBackdrop(color) }

/**
 * The backdrop a glass surface should actually sample.
 *
 * Resolves [backdrop] to the flat window background when the platform cannot
 * blur it — see the note on this file about pre-Android-12 devices.
 */
@Composable
fun rememberEffectiveGlassBackdrop(backdrop: GlassBackdrop?): GlassBackdrop {
    val flat = rememberSolidGlassBackdrop(MaterialTheme.colorScheme.background)
    return if (isGlassBlurSupported && backdrop != null) backdrop else flat
}

// MARK: - Surface

/**
 * The opaque equivalent of a glass surface.
 *
 * It is the wash composited over the window background, which is exactly what
 * the glass would look like over a flat backdrop — same pixels, no sampling, no
 * blur, no shader. That is the fallback whenever [isLiquidGlassEnabled] is
 * false: performance mode, or a device that cannot blur.
 */
@Composable
fun glassOpaqueColor(surface: Color): Color =
    surface.compositeOver(MaterialTheme.colorScheme.background)

/**
 * Draws this element as a liquid-glass surface.
 *
 * @param backdrop what to refract. Defaults to [LocalGlassBackdrop], which a
 *   screen provides for the surfaces nested inside it.
 * @param surface the wash drawn over the refracted content. When glass is
 *   disabled this becomes an opaque fill instead.
 * @param highlight the specular rim. [Highlight.Default] is the library's edge
 *   light, and it is most of what makes the surface read as glass over a dark
 *   background.
 * @param layerBlock lets the caller animate the surface itself (press scale,
 *   drag lean) without moving the sampled backdrop with it.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: GlassBackdrop? = LocalGlassBackdrop.current,
    shape: Shape = GlassDefaults.CardShape,
    surface: Color = GlassDefaults.Surface,
    blurRadius: Dp = GlassDefaults.BlurRadius,
    refractionHeight: Dp = GlassDefaults.RefractionHeight,
    refractionAmount: Dp = GlassDefaults.RefractionAmount,
    highlight: Highlight? = Highlight.Default,
    shadow: Shadow? = null,
    innerShadow: InnerShadow? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null
): Modifier {
    val glassShape = shape
    val enabled = isLiquidGlassEnabled
    // No glass available: an opaque surface, not a translucent one. A
    // translucent fill with nothing blurred behind it is what made the old UI
    // read as having no material at all.
    val opaque = glassOpaqueColor(surface)
    val effectiveBackdrop = rememberEffectiveGlassBackdrop(backdrop)
    val surfaceColor = surface

    return if (!enabled) {
        this
            .clip(glassShape)
            .background(opaque)
    } else {
        this.drawGlassBackdrop(
            backdrop = effectiveBackdrop,
            shape = { glassShape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                lens(refractionHeight.toPx(), refractionAmount.toPx())
            },
            highlight = highlight?.let { h -> { h } },
            shadow = shadow?.let { s -> { s } },
            innerShadow = innerShadow?.let { s -> { s } },
            layerBlock = layerBlock,
            onDrawSurface = { drawRect(surfaceColor) }
        )
    }
}

// MARK: - Scrim

/**
 * The progressive header backdrop — a vertical gradient that dissolves content
 * into the app's black background.
 *
 * This is not glass: it is the fallback for the pinned header on devices where
 * the GPU cannot blur at all (below Android 12), where a Glur would show the
 * feed *sharp* under a fade instead of frosted. Everywhere else the header uses
 * the Glur.
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
