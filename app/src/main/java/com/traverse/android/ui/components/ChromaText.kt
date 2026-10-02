package com.traverse.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.traverse.android.ui.theme.ColorPalette
import com.traverse.android.ui.theme.rememberPalette
import kotlinx.coroutines.delay

/**
 * The sweep's colours come from the **active palette**, not from a hardcoded band.
 *
 * There used to be a `ChromaBand` object here holding the five website hexes. That made the sweep
 * the one thing in the app that ignored the user's palette choice — defensible only while nobody
 * could tell, because the default palette *is* the band. Now that Traverse (the chroma band,
 * renamed) is the default and the other palettes are one tap away, a hardcoded sweep meant picking
 * Monochrome gave you grey everywhere except the one animation that is supposed to carry the brand.
 *
 * So the band is no longer a constant here. It is the Traverse palette's five colours, documented
 * at its definition in `ColorPalette.allPalettes`, and the sweep reads whichever palette is
 * selected. The same five hexes by default, and a coherent look on any other.
 */

/**
 * Tracks which chroma sweeps have already played during the current visit to a screen.
 *
 * **The rule is once per visit to a screen.** A sweep plays when the screen is first shown, does
 * *not* play again while the user stays on it — including when a `LazyColumn` recycles the row and
 * its `LaunchedEffect` re-runs — and plays again when they come back from another screen.
 *
 * Two things this is deliberately not:
 *
 * - **Not once per session.** The website latches each sweep forever, which is right for a
 *   scrolling marketing page you read once. The home feed is somewhere you return to many times a
 *   day; a sweep that never plays again stops being a brand moment and becomes decoration.
 * - **Not on every value change.** The streak number used to re-sweep whenever the streak changed
 *   (via `key(streak)`), which fires while the user is still looking at the card. That is the
 *   opposite of an entrance.
 *
 * **Ownership matters.** The *screen* owns the gate, not the card. A card inside a `LazyColumn` is
 * disposed once it scrolls out of the keep-alive window, so a gate held there would reset on scroll
 * and the sweep would replay every time the user scrolled back to the top.
 *
 * Reset semantics differ by platform. Here they come free: Compose Navigation only composes the
 * current destination, so the screen holding this gate is disposed on navigate-away and `remember`
 * hands back a fresh one on return. On iOS `NavigationStack` keeps the root mounted, so `HomeView`
 * has to reset its gate explicitly from the pushed destinations' `onDisappear`.
 */
class SweepGate {
    private val played = mutableSetOf<String>()

    /**
     * Returns `true` the first time this key is asked for during the current visit, `false` every
     * time after. Inserts as a side effect, so call it exactly once per appearance.
     */
    fun claim(key: String): Boolean = played.add(key)

    /** Call when the owning screen is re-entered, so its sweeps play again. */
    fun reset() {
        played.clear()
    }
}

/**
 * Text that materialises out of the chroma band and settles into a solid colour.
 *
 * Port of the iOS `ChromaText`, which is itself a port of the website's `ChromaText` /
 * `chroma-sweep` (LeetFeedback, `src/components/ui/textRenderAppear.tsx` plus the keyframes in
 * `src/index.css`). "textrenderappear" and "chromasweep" are the same feature.
 *
 * **The mechanic.** The gradient is three times the width of the text, and the visible window is
 * the middle third of it. At progress 0 the window sits on the gradient's *right* third — which
 * is transparent, so the text is invisible. As progress runs to 1 the window slides left, through
 * the band, and lands on the left third, which is the resting colour. Continuous, so no discrete
 * swap is needed at the end.
 *
 * **The trap.** The site declares `-webkit-text-fill-color: transparent` at *both* 0% and 95% of
 * its keyframe specifically so the fill change is confined to the last 5%. Declare it only at the
 * end and the browser interpolates it across the whole duration, leaving the text faintly visible
 * the entire sweep. The equivalent mistake here is animating the text colour and the gradient at
 * the same time — so the resting colour lives inside the gradient's left third and there is
 * nothing to interpolate.
 *
 * **The blur.** `filter: blur(1px)` → `blur(0)` is what sells the materialising rather than just
 * the colouring-in. `Modifier.blur` needs API 31+; below that it is a no-op and the sweep still
 * reads. minSdk here is 26, so on older devices this is a slightly flatter version of the same
 * animation rather than a broken one.
 *
 * **Note on the site's header comment.** `textRenderAppear.tsx` opens with a "CRITICAL: DO NOT
 * BREAK THIS ANIMATION" block — no inline animation styles, one `<style>` tag at the parent
 * level, no dynamically injected styles. Every one of those rules is a Tailwind source-scanning
 * or CSS-injection constraint. None of them exist in Compose, and none of them should be
 * recreated here. What is worth porting is the mechanic.
 *
 * @param restingColor where the sweep lands. Also the gradient's left third, so the handoff is
 *   seamless. White by default.
 */
@Composable
fun ChromaText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    restingColor: Color = Color.White,
    delayMillis: Long = 100,
    durationMillis: Int = 1200,
    gate: SweepGate? = null,
    sweepKey: String = "default",
    /**
     * The colours the sweep travels through, read from the palette rather than baked in. The default
     * palette (Traverse) *is* the chroma band, so the out-of-the-box look is unchanged — but picking
     * another palette now recolours the sweep instead of leaving it as the one part of the app that
     * ignores the choice.
     */
    palette: ColorPalette = rememberPalette(),
) {
    val progress = remember { Animatable(0f) }

    // Brush offsets are in pixels, not fractions, so the text has to report its own width before
    // the gradient can be built.
    var width by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // Already played on this visit — jump to the end state and skip the animation. The row can
        // be recreated by a `LazyColumn` while the gate lives on at screen level, which is exactly
        // the case this guards.
        if (gate != null && !gate.claim(sweepKey)) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        delay(delayMillis)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        )
    }

    val p = progress.value

    val fill: Brush = when {
        p >= 0.999f -> SolidColor(restingColor)
        // Before the first layout pass the text has no width and the gradient would be
        // degenerate. Transparent for that one frame is correct — that is what the sweep looks
        // like at progress 0 anyway.
        width <= 0f -> SolidColor(Color.Transparent)
        else -> {
            // The gradient spans three text-widths. Left third = resting colour, middle fifth = the
            // palette, right third = clear. Sliding x0 from -2 to 0 walks the visible window (which
            // is always the text's own 0...width) from the right third to the left third.
            val x0 = (-2f + 2f * p) * width

            // The window is a fixed 0.20 wide and the step is derived from the colour count, so a
            // five-colour palette lands on exactly the original 0.40 / 0.45 / 0.50 / 0.55 / 0.60 —
            // while a custom palette with three or eight colours still fills the window instead of
            // crowding one end of it.
            val band = palette.swiftUIColors
            val step = if (band.size > 1) 0.20f / (band.size - 1) else 0f
            val stops = buildList {
                add(0.0000f to restingColor)
                add(0.3333f to restingColor)
                band.forEachIndexed { index, color ->
                    add((0.40f + index * step) to color)
                }
                add(0.6667f to Color.Transparent)
                add(1.0000f to Color.Transparent)
            }.toTypedArray()

            Brush.linearGradient(
                colorStops = stops,
                start = Offset(x0, 0f),
                end = Offset(x0 + 3f * width, 0f),
            )
        }
    }

    Text(
        text = text,
        style = style.copy(brush = fill),
        modifier = modifier
            .onSizeChanged { width = it.width.toFloat() }
            .blur((1f - p).coerceIn(0f, 1f) * 1.dp),
    )
}
