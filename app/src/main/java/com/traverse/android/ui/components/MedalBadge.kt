package com.traverse.android.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.traverse.android.R

/**
 * Degrees of lean at full drag. Apple's medals lean further than you'd expect — a shallow
 * tilt reads as a wobble rather than a physical object.
 */
private const val MAX_TILT = 32f

/** Larger values flatten the perspective; 12 keeps a believable amount of depth. */
private const val CAMERA_DISTANCE = 12f

/**
 * An Apple Fitness style award badge.
 *
 * The badge leans in 3D while the user holds and drags on it. This is deliberately **not**
 * sensor driven: the real Fitness app tilts its medals with device motion, but here the tilt
 * is a press-and-drag so it is discoverable, works with the phone flat on a table, and never
 * fights the surrounding scroll container.
 *
 * @param medal drawable slug from [com.traverse.android.data.MedalCatalog], resolved as
 *   `medal_<slug>` in `res/drawable-nodpi`.
 * @param interactive pass false for badges inside a clickable card so the drag doesn't
 *   swallow the tap.
 */
@Composable
fun MedalBadge(
    medal: String,
    modifier: Modifier = Modifier,
    unlocked: Boolean = true,
    size: Dp = 120.dp,
    interactive: Boolean = true
) {
    val painter = painterResource(id = medalDrawableId(medal))
    val localDensity = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    var drag by remember { mutableStateOf(Offset.Zero) }
    // Springs back to flat on release; StiffnessHigh keeps it tracking the finger 1:1 while
    // the bouncy damping gives the badge a little overshoot as it settles.
    val tilt by animateOffsetAsState(
        targetValue = drag,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "medalTilt"
    )

    val px = with(localDensity) { size.toPx() }.coerceAtLeast(1f)
    val rotationX = -(tilt.y / px) * MAX_TILT
    val rotationY = (tilt.x / px) * MAX_TILT
    val magnitude = (kotlin.math.hypot(tilt.x, tilt.y) / px).coerceAtMost(1f)
    val lift = 1f + magnitude * 0.05f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                scaleX = lift
                scaleY = lift
                // `density` here is GraphicsLayerScope's own Float density.
                cameraDistance = CAMERA_DISTANCE * density
            }
            .then(
                if (interactive) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { drag = Offset.Zero },
                            onDragCancel = { drag = Offset.Zero }
                        ) { change, dragAmount ->
                            change.consume()
                            drag += dragAmount
                        }
                    }
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = if (unlocked) "Earned award badge" else "Locked award badge"
            }
    ) {
        if (unlocked) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // The renders already carry their own baked specular, so the moving highlight is
            // only worth drawing where the user can actually tilt the badge.
            if (interactive) {
                SpecularSheen(painter = painter, tilt = tilt, sizePx = px)
            }
        } else {
            // Unearned badges read as a drained, dark relief — same silhouette, no colour.
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = lockedColorFilter()
            )
        }
    }
}

/**
 * A fixed light source: as the badge leans, the highlight sweeps across its face. The
 * gradient is masked by the badge's own alpha so the highlight never spills onto the card.
 */
@Composable
private fun SpecularSheen(painter: Painter, tilt: Offset, sizePx: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Offscreen so the DstIn mask below applies inside this layer only.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val reach = sizePx * 0.62f
        val offsetX = -tilt.x * 0.85f
        val offsetY = -tilt.y * 0.85f
        val alpha = (0.18f + (kotlin.math.hypot(tilt.x, tilt.y) / sizePx).coerceAtMost(1f) * 0.37f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width / 2f + offsetX, size.height / 2f + offsetY)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = alpha),
                                Color.White.copy(alpha = alpha * 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = reach
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height)
                    )
                }
        )
        // Keep only the part of the highlight that lands on the badge itself.
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { blendMode = BlendMode.DstIn },
            contentScale = ContentScale.Fit
        )
    }
}

/** Desaturate, then darken — the look of a badge that hasn't been earned yet. */
@Composable
private fun lockedColorFilter(): ColorFilter {
    val scale = 0.62f
    return remember(scale) {
        val values = ColorMatrix().apply { setToSaturation(0f) }.values.copyOf()
        // Rows 0..14 are R, G and B; leaving the alpha row alone keeps the silhouette crisp.
        for (i in 0 until 15) values[i] *= scale
        ColorFilter.colorMatrix(ColorMatrix(values))
    }
}

/** The thin capsule Apple draws under an in-progress badge. */
@Composable
fun MedalProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    tint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(width = width, height = 4.dp)
            .background(Color.White.copy(alpha = 0.16f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(width = (width * fraction.coerceIn(0f, 1f)).coerceAtLeast(2.dp), height = 4.dp)
                .background(tint, CircleShape)
        )
    }
}

/**
 * Resolve a medal slug to its drawable. Unknown slugs fall back to the first badge so a
 * badge always renders rather than blowing up on a missing resource.
 */
@Composable
fun medalDrawableId(slug: String): Int {
    val context = LocalContext.current
    val name = "medal_${slug.ifEmpty { "new_year_2017" }}"
    val id = remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
    return id.takeIf { it != 0 } ?: R.drawable.medal_new_year_2017
}
