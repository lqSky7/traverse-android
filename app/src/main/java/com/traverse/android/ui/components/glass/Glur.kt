package com.traverse.android.ui.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.traverse.android.ui.components.glass.effects.blur

/**
 * The Android port of Glur's backdrop blur — `GlurView` on iOS.
 *
 * iOS `GlurView` is a transparent overlay that blurs whatever is rendered
 * *behind* it, which is what lets the pinned header dissolve the feed as it
 * scrolls underneath. Android has no such primitive, so the same effect is
 * built here on the vendored backdrop library: the screen captures its
 * scrolling content into a [GlassBackdrop], and this modifier redraws that
 * capture, blurred, on top of it.
 *
 * The mask ramp is the part that makes it read as a dissolve rather than as a
 * frosted panel. iOS shapes the blur with a `GlurMask.linear` ramp; Android's
 * `RenderEffect` blur is uniform, so the ramp is applied to how much of the
 * blurred band survives instead — full strength at the top edge, nothing at
 * the bottom. Same stops, same look, no hard line anywhere.
 */

/**
 * Which slice of the header ramp a surface owns.
 *
 * Two pinned headers stack on some screens (heading above, glance cards
 * below). Each takes its own slice of one continuous ramp, so the pair reads
 * as a single dissolve with no band between them. Mirrors `ProgressiveBlur
 * .Segment` on iOS.
 */
enum class GlurSegment {
    /** Heading row: strongest blur and darkest tint. */
    Top,

    /** Cards row: continues the heading's ramp down to zero. */
    Bottom,

    /** A whole header in one view (Home, Revisions, Friends). */
    Single
}

/** The blur ramp, as the fraction of the blurred band that survives. */
private fun GlurSegment.fadeStops(): Array<Pair<Float, Color>> = when (this) {
    GlurSegment.Top -> arrayOf(
        0f to Color.White,
        1f to Color.White.copy(alpha = 0.65f)
    )

    GlurSegment.Bottom -> arrayOf(
        0f to Color.White.copy(alpha = 0.65f),
        0.5f to Color.White.copy(alpha = 0.4f),
        1f to Color.Transparent
    )

    // iOS shapes the blur by multiplying the *radius*, so the whole band stays
    // frosted and only softens toward the bottom. Android's RenderEffect blur is
    // uniform, so the ramp has to ride on how much of the blurred copy survives
    // instead — which means the stops must stay near-opaque well past the
    // halfway point, or the sharp feed shows through the ramp and the whole
    // header reads as barely blurred.
    GlurSegment.Single -> arrayOf(
        0f to Color.White,
        0.5f to Color.White.copy(alpha = 0.94f),
        0.82f to Color.White.copy(alpha = 0.62f),
        1f to Color.Transparent
    )
}

/**
 * The gentle tint behind the blur.
 *
 * Without it a blur is invisible against this app's black background — that is
 * the whole reason the previous "glass" read as nothing. It stays light: a
 * heavy dark bar was explicitly rejected, so the darkest stop is 0.5 and it is
 * always fully clear by the bottom edge.
 */
private fun GlurSegment.tintStops(): Array<Pair<Float, Color>> = when (this) {
    GlurSegment.Top -> arrayOf(
        0f to Color.Black.copy(alpha = 0.5f),
        1f to Color.Black.copy(alpha = 0.32f)
    )

    GlurSegment.Bottom -> arrayOf(
        0f to Color.Black.copy(alpha = 0.32f),
        0.5f to Color.Black.copy(alpha = 0.14f),
        1f to Color.Transparent
    )

    GlurSegment.Single -> arrayOf(
        0f to Color.Black.copy(alpha = 0.5f),
        0.45f to Color.Black.copy(alpha = 0.28f),
        0.8f to Color.Black.copy(alpha = 0.1f),
        1f to Color.Transparent
    )
}

/**
 * Draws the blurred live content behind this surface, ramped out toward the
 * bottom edge, over a gentle tint.
 *
 * @param backdrop the capture of the content scrolling behind this surface.
 *   The caller owns it — see [Glur].
 * @param radius the blur radius where the ramp is fully applied.
 * @param segment which slice of the ramp this surface covers.
 * @param tint whether to lay the dark tint under the blur. It exists so a blur
 *   over the app's black background is visible at all; a surface that already
 *   paints its own overlay (the profile banner) turns it off so the two do not
 *   stack into a gradient nobody asked for.
 */
fun Modifier.glurBackdrop(
    backdrop: GlassBackdrop,
    radius: Dp = 16.dp,
    segment: GlurSegment = GlurSegment.Single,
    tint: Boolean = true
): Modifier = this.drawPlainGlassBackdrop(
    backdrop = backdrop,
    shape = { RectangleShape },
    effects = {
        blur(radius.toPx())
    },
    // The ramp has to be applied where the capture is recorded, not after it
    // is composited: `onDrawBackdrop` runs inside the layer the backdrop is
    // recorded into, so the DstIn below can only ever touch this capture. Run
    // from outside and it would blend against the whole screen instead, wiping
    // out the feed it is meant to be blurring.
    onDrawBackdrop = { drawBlurred ->
        drawBlurred()

        // Keep the blurred band only where the ramp says so.
        drawRect(
            brush = Brush.verticalGradient(*segment.fadeStops()),
            blendMode = BlendMode.DstIn
        )

        // Then the tint, over what survived.
        if (tint) {
            drawRect(brush = Brush.verticalGradient(*segment.tintStops()))
        }
    }
)

/**
 * A transparent overlay that blurs the content behind it — the Android
 * counterpart of iOS `GlurView`.
 *
 * Give it the size of the surface it backs, typically `matchParentSize()`
 * inside the header's `Box`:
 *
 * ```kotlin
 * Box {
 *     LazyColumn(Modifier.fillMaxSize().layerGlassBackdrop(backdrop)) { … }
 *     GlanceScreenHeader(title, cards, backdrop = backdrop, …)
 * }
 * ```
 */
@Composable
fun Glur(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    segment: GlurSegment = GlurSegment.Single,
    tint: Boolean = true
) {
    // Deliberately empty: this only paints, it has nothing to lay out. Give it
    // `Modifier.matchParentSize()` to size it to the surface it backs.
    Box(modifier = modifier.glurBackdrop(backdrop, radius, segment, tint)) {}
}

/*
 * Every glass *surface* in the app now goes through `Modifier.liquidGlass`
 * (see Glass.kt), which is the same library pipeline with the specular rim,
 * inner shadow and surface wash that make it read as glass. The Glur above is
 * the other half of the language: it is the pinned header's *backdrop*, not a
 * surface of its own.
 */
