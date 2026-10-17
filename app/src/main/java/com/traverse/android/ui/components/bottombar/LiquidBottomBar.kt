package com.traverse.android.ui.components.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import com.traverse.android.ui.components.glass.GlassBackdrop
import com.traverse.android.ui.components.glass.GlassDefaults
import com.traverse.android.ui.components.glass.drawGlassBackdrop
import com.traverse.android.ui.components.glass.effects.blur
import com.traverse.android.ui.components.glass.effects.lens
import com.traverse.android.ui.components.glass.effects.vibrancy
import com.traverse.android.ui.components.glass.glassOpaqueColor
import com.traverse.android.ui.components.glass.highlight.Highlight
import com.traverse.android.ui.components.glass.isLiquidGlassEnabled
import com.traverse.android.ui.components.glass.rememberEffectiveGlassBackdrop
import com.traverse.android.ui.components.glass.shadow.InnerShadow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The bottom tab bar, ported from ClearPDF's `LiquidBottomTabs`.
 *
 * One glass capsule, three layers stacked on it:
 *
 * 1. the bar itself — real backdrop glass, so the screen refracts through it;
 * 2. the selected pill — a lit capsule of the same glass that slides between
 *    tabs, swells under the finger, and can be dragged;
 * 3. the icons — crisp, on top of everything.
 *
 * The icons are the one place this deliberately departs from ClearPDF. There
 * the pill is drawn last and refracts a hidden, accent-tinted duplicate of the
 * bar, so the selected icon is *inside* the pill's backdrop — which blurs it
 * into mush and, over a black background, leaves it invisible. Putting the pill
 * underneath keeps the icon sharp while the pill still reads as lit glass.
 *
 * Only the icons are ours; the bar, the pill and the motion are ClearPDF's
 * design.
 */

/** One tab in the bar. */
data class LiquidBarItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val badge: Int? = null
)

/**
 * @param accent tints the pill's selected sheen and the ghost the pill refracts.
 * @param surface the capsule's own wash — it is what keeps the bar visible over
 *   the app's black background.
 */
@Composable
fun LiquidBottomBar(
    items: List<LiquidBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: GlassBackdrop,
    accent: Color,
    surface: Color,
    selectedSheen: Color,
    unselectedTint: Color,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 64.dp
) {
    require(items.isNotEmpty()) { "LiquidBottomBar requires at least one item." }

    val tabsCount = items.size
    val glassBackdrop = rememberEffectiveGlassBackdrop(backdrop)

    val contentHeight = (barHeight - 8.dp).coerceAtLeast(0.dp)
    val animationScope = rememberCoroutineScope()

    // Performance mode, or a device that cannot blur: the same geometry, drawn
    // opaque. No capture, no blur pass, no refraction shader.
    val glassOn = isLiquidGlassEnabled
    val opaqueBar = glassOpaqueColor(surface)
    val opaquePill = accent.copy(alpha = 0.16f)
        .compositeOver(selectedSheen.compositeOver(opaqueBar))

    val selectionState = rememberUpdatedState(selectedIndex)
    val selectState = rememberUpdatedState(onSelect)
    val pill = remember(animationScope, tabsCount) {
        TabPillDrag(
            scope = animationScope,
            initialIndex = selectionState.value,
            tabCount = tabsCount,
            currentSelection = { selectionState.value },
            onSelect = { selectState.value(it) }
        )
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
        pill.animateToIndex(selectedIndex)
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        val tabWidthPx = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        // While a drag is in flight the whole bar leans a few dp toward it, so
        // the pill looks like it is pulling the glass along. Eased out, so the
        // lean saturates instead of growing without bound.
        val panelOffset by remember(density, constraints.maxWidth) {
            derivedStateOf {
                if (constraints.maxWidth == 0) return@derivedStateOf 0f
                val fraction = (pill.dragOffset / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }

        // 1. The bar's glass.
        Row(
            modifier = Modifier
                .graphicsLayer { translationX = panelOffset }
                .then(
                    if (glassOn) {
                        Modifier.drawGlassBackdrop(
                            backdrop = glassBackdrop,
                            shape = { GlassDefaults.Capsule },
                            shadow = null,
                            effects = {
                                // Half ClearPDF's 8 dp frost: the 24x24 lens is
                                // what gives the bar its edge.
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(24.dp.toPx(), 24.dp.toPx())
                            },
                            highlight = { Highlight.Default },
                            onDrawSurface = { drawRect(surface) }
                        )
                    } else {
                        Modifier
                            .clip(GlassDefaults.Capsule)
                            .background(opaqueBar)
                    }
                )
                .height(barHeight)
                .fillMaxWidth()
                .padding(4.dp)
        ) {}

        // 2. The selected pill, between the bar's glass and the icons.
        //
        // It has to sit *under* the icons. On top, its own frost blurred the
        // selected tab's icon into mush — the pill was sampling a backdrop that
        // had the icon in it, and blurring it. Underneath, the icon stays
        // crisp and the pill still reads as a lit capsule of the same glass.
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) pill.value * tabWidthPx + panelOffset
                        else size.width - (pill.value + 1f) * tabWidthPx + panelOffset
                }
                .then(
                    if (glassOn) {
                        Modifier.drawGlassBackdrop(
                            backdrop = glassBackdrop,
                            shape = { GlassDefaults.Capsule },
                            effects = {
                                val progress = pill.pressProgress
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(
                                    refractionHeight = 10.dp.toPx() * progress,
                                    refractionAmount = 14.dp.toPx() * progress,
                                    chromaticAberration = true
                                )
                            },
                            highlight = { Highlight.Default },
                            shadow = null,
                            innerShadow = {
                                val progress = pill.pressProgress
                                InnerShadow(radius = 8.dp * progress, alpha = progress)
                            },
                            layerBlock = {
                                // A surface with no size yet divides by zero,
                                // which the platform rejects on older Android
                                // with a crash.
                                if (size.width > 0f && size.height > 0f) {
                                    val scale = pill.pressScale
                                    scaleX = scale
                                    scaleY = scale
                                }
                            },
                            onDrawSurface = {
                                val progress = pill.pressProgress
                                // A lit capsule of the same glass, not a
                                // pasted-on chip. Constant, because the pill is
                                // always the selection; the accent deepens while
                                // the bar is held.
                                drawRect(selectedSheen)
                                drawRect(accent.copy(alpha = 0.16f + 0.10f * progress))
                            }
                        )
                    } else {
                        Modifier
                            .graphicsLayer {
                                val scale = pill.pressScale
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(GlassDefaults.Capsule)
                            .background(opaquePill)
                    }
                )
                .height(contentHeight)
                .fillMaxWidth(1f / tabsCount)
        )

        // 3. The icons, crisp, above everything else.
        Row(
            modifier = Modifier
                .height(contentHeight)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                // On the padded row, so `size.width` is exactly the tab strip:
                // one tab is `size.width / tabsCount`.
                .then(pill.modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BarTab(
                    item = item,
                    selected = index == currentIndex,
                    iconScale = { 1f + 0.18f * pill.pressProgress },
                    tint = if (index == currentIndex) accent else unselectedTint,
                    badgeColor = badgeColor,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BarTab(
    item: LiquidBarItem,
    selected: Boolean,
    iconScale: () -> Float,
    tint: Color,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(GlassDefaults.Capsule)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .graphicsLayer {
                val scale = iconScale()
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )

            val badge = item.badge
            if (badge != null && badge > 0) {
                Text(
                    text = if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-5).dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * The pill's motion: a spring that slides between tabs and can be dragged.
 *
 * A trimmed port of ClearPDF's `DampedDragAnimation` — same spring feel and the
 * same drag-to-scrub behaviour, without the velocity-squash and long-press
 * bloom, which need the playground's motion settings that this app does not
 * expose.
 */
@Stable
private class TabPillDrag(
    private val scope: CoroutineScope,
    initialIndex: Int,
    private val tabCount: Int,
    private val currentSelection: () -> Int,
    private val onSelect: (Int) -> Unit
) {
    private val valueAnimation = Animatable(initialIndex.toFloat())
    private val pressAnimation = Animatable(0f)
    private val offsetAnimation = Animatable(0f)

    val value: Float get() = valueAnimation.value
    val pressProgress: Float get() = pressAnimation.value
    val dragOffset: Float get() = offsetAnimation.value

    /** ClearPDF's press swell, tempered so the capsule does not balloon. */
    val pressScale: Float get() = 1f + 0.15f * pressAnimation.value

    private val maxIndex = (tabCount - 1).toFloat()

    fun animateToIndex(index: Int) {
        scope.launch {
            valueAnimation.animateTo(
                targetValue = index.toFloat().coerceIn(0f, maxIndex),
                animationSpec = spring(1f, 1000f, 0.001f)
            )
        }
    }

    private fun press() {
        scope.launch { pressAnimation.animateTo(1f, spring(0.66f, 620f, 0.001f)) }
    }

    private fun settle() {
        val target = valueAnimation.value.roundToInt().coerceIn(0, tabCount - 1)
        scope.launch {
            launch { pressAnimation.animateTo(0f, spring(0.5f, 380f, 0.001f)) }
            launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            valueAnimation.animateTo(target.toFloat(), spring(1f, 1000f, 0.001f))
            if (target != currentSelection()) onSelect(target)
        }
    }

    val modifier: Modifier = Modifier.pointerInput(tabCount) {
        detectDragGestures(
            onDragStart = { press() },
            onDragEnd = { settle() },
            onDragCancel = { settle() },
            onDrag = { change, dragAmount ->
                change.consume()
                // The gesture lives on the tab strip, so one tab is the strip
                // divided by the tab count.
                val tabWidth = size.width / tabCount.toFloat()
                if (tabWidth > 0f) {
                    scope.launch {
                        valueAnimation.snapTo(
                            (valueAnimation.value + dragAmount.x / tabWidth)
                                .coerceIn(0f, maxIndex)
                        )
                    }
                    scope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        )
    }
}
