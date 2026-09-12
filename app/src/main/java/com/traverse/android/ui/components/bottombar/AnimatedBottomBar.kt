/*
 * Vendored from https://github.com/MehdiSekoba/AnimatedBottomBar (module: animatedbottombar, v1.0.0)
 * Copyright 2026 Mehdi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Local changes vs. upstream: the two `delay(Duration)` calls were swapped for millisecond
 * overloads so the file compiles against this module's coroutines version without opt-ins.
 */

package com.traverse.android.ui.components.bottombar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay

@Composable
fun AnimatedBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: AnimatedBottomBarState = rememberAnimatedBottomBarState(),
    style: BottomBarStyle = BottomBarDefaults.style(),
    animationSpec: BottomBarAnimationSpec = BottomBarDefaults.animationSpec(),
) {
    require(items.isNotEmpty()) { "AnimatedBottomBar requires at least one item." }

    val density = LocalDensity.current
    val itemCount = items.size

    val safeSelected = selectedIndex.coerceIn(0, itemCount - 1)

    val animatedSlot: Float by animateFloatAsState(
        targetValue = safeSelected.toFloat(),
        animationSpec = animationSpec.indicatorSpec,
        label = "indicatorSlot",
    )

    val hillHeightAnimated: Float by animateFloatAsState(
        targetValue = style.indicatorHeight.value,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hillRise"
    )

    // Controls the lifecycle of the ejected droplet (0f = at the base, 1f = hit the icon and vanished)
    val splashProgress = remember { Animatable(0f) }

    // Trigger the splash whenever the selected item changes
    LaunchedEffect(safeSelected) {
        splashProgress.snapTo(0f) // Reset the droplet

        // Wait slightly longer for the base hill to slide under the icon
        delay(100L)

        splashProgress.animateTo(
            targetValue = 1f,
            // Slower, smoother travel time
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(style.barHeight)
            .selectableGroup(),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val contentPaddingPx = with(density) { style.contentPadding.toPx() }
        val hillHalfWidthPx = with(density) { style.indicatorSize.toPx() } / 2f
        val hillHeightPx = with(density) { style.indicatorHeight.toPx() }

        // Grab layout direction for RTL support
        val layoutDirection = LocalLayoutDirection.current

        val slotWidth = (widthPx - contentPaddingPx * 2f) / itemCount
        fun slotCenterX(slot: Float): Float {
            val calculatedX = contentPaddingPx + slotWidth * (slot + 0.5f)
            return if (layoutDirection == LayoutDirection.Rtl) {
                widthPx - calculatedX
            } else {
                calculatedX
            }
        }

        val hillCenterX = slotCenterX(animatedSlot)

        // ---- Container + Animated Hill + Splash ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.barHeight)
                .clip(RoundedCornerShape(style.containerShapeRadius))
                .drawBehind {
                    drawRect(color = style.containerColor)

                    // 1. Draw the base hill
                    val hill = buildIndicatorHillPath(
                        size = size,
                        centerX = hillCenterX,
                        halfWidth = hillHalfWidthPx,
                        height = hillHeightAnimated * hillHeightPx / style.indicatorHeight.value,
                    )
                    drawPath(path = hill, color = style.indicatorColor)

                    // 2. Draw the ejected liquid droplet that swells to cover the icon
                    if (splashProgress.value > 0f && splashProgress.value < 1f) {
                        val startY = size.height - hillHeightPx
                        val endY = size.height / 2f // Middle of the bar (where the icon is)

                        val currentY = lerp(startY, endY, splashProgress.value)

                        // Make the radius big enough to cover a standard icon (80% of the icon size)
                        val maxRadius = with(density) { style.iconSize.toPx() * 0.5f }
                        // The droplet grows as it travels, peaks at 70% of the animation, then shrinks
                        val currentRadius = if (splashProgress.value < 0.7f) {
                            lerp(0f, maxRadius, splashProgress.value / 0.7f)
                        } else {
                            lerp(maxRadius, 0f, (splashProgress.value - 0.7f) / 0.3f)
                        }

                        drawCircle(
                            color = style.indicatorColor,
                            radius = currentRadius,
                            center = Offset(x = hillCenterX, y = currentY)
                        )
                    }
                },
        )

        // ---- Items Row ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.barHeight)
                .padding(horizontal = style.contentPadding),
        ) {
            items.forEachIndexed { index, item ->
                BottomBarItemContent(
                    item = item,
                    selected = index == safeSelected,
                    style = style,
                    animationSpec = animationSpec,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

/**
 * Single tappable item with lift animation
 */
@Composable
private fun BottomBarItemContent(
    item: BottomBarItem,
    selected: Boolean,
    style: BottomBarStyle,
    animationSpec: BottomBarAnimationSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Delay the visual swap so the liquid covers the icon first
    var visuallySelected by remember { mutableStateOf(selected) }

    LaunchedEffect(selected) {
        if (selected && !visuallySelected) {
            delay(250L) // Wait for the liquid splash to travel and cover the icon
            visuallySelected = true
        } else if (!selected) {
            visuallySelected = false
        }
    }

    val targetColor = when {
        !item.enabled -> style.disabledIconColor
        visuallySelected -> style.selectedIconColor
        else -> style.unselectedIconColor
    }

    val iconColor: Color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(150),
        label = "iconColor",
    )

    val iconScale: Float by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = animationSpec.iconScaleSpec,
        label = "iconScale",
    )

    // Icon lift animation
    val iconOffsetY: Float by animateFloatAsState(
        targetValue = if (selected) -10f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconLift",
    )

    val stateDesc = if (selected) "Selected" else "Not selected"
    val badgeSuffix = item.badge?.takeIf { it.isVisible() }?.accessibilitySuffix()
    val description = if (badgeSuffix != null) "${item.label}, $badgeSuffix" else item.label

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = stateDesc
            }
            .selectable(
                selected = selected,
                enabled = item.enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedIcon(
                item = item,
                selected = visuallySelected, // Use the delayed state for the asset swap
                tint = iconColor,
                style = style,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = iconOffsetY
                },
            )

            Text(
                text = item.label,
                color = iconColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
