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
 */
package com.traverse.android.ui.components.bottombar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defaults for [AnimatedBottomBar]. Use the factory functions here to build [BottomBarStyle] and
 * [BottomBarAnimationSpec] instances that are pre-wired to the current [MaterialTheme], overriding
 * only what you need.
 */
object BottomBarDefaults {

    /** Default height of the bar container. */
    val BarHeight: Dp = 72.dp

    /** Default width of the indicator hill's base along the bottom edge. */
    val IndicatorSize: Dp = 56.dp

    /**
     * Builds a [BottomBarStyle]. Any [Color.Unspecified] argument is resolved against the current
     * [MaterialTheme]; any [Dp.Unspecified] dimension falls back to the documented default.
     */
    @Composable
    @ReadOnlyComposable
    fun style(
        containerColor: Color = Color.Unspecified,
        indicatorColor: Color = Color.Unspecified,
        selectedIconColor: Color = Color.Unspecified,
        unselectedIconColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
        badgeColor: Color = Color.Unspecified,
        badgeContentColor: Color = Color.Unspecified,
        containerShapeRadius: Dp = Dp.Unspecified,
        iconSize: Dp = Dp.Unspecified,
        indicatorSize: Dp = Dp.Unspecified,
        indicatorHeight: Dp = Dp.Unspecified,
        barHeight: Dp = Dp.Unspecified,
        contentPadding: Dp = Dp.Unspecified,
        labelTextStyle: TextStyle = TextStyle.Default,
        badgeTextStyle: TextStyle = TextStyle.Default,
    ): BottomBarStyle {
        val scheme = MaterialTheme.colorScheme
        return BottomBarStyle(
            containerColor = containerColor.orElse(scheme.surface),
            indicatorColor = indicatorColor.orElse(scheme.primary),
            selectedIconColor = selectedIconColor.orElse(scheme.primary),
            unselectedIconColor = unselectedIconColor.orElse(scheme.primary),
            disabledIconColor = disabledIconColor.orElse(
                scheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
            badgeColor = badgeColor.orElse(scheme.error),
            badgeContentColor = badgeContentColor.orElse(scheme.onError),
            containerShapeRadius = containerShapeRadius.orElse(28.dp),
            iconSize = iconSize.orElse(24.dp),
            indicatorSize = indicatorSize.orElse(IndicatorSize),
            indicatorHeight = indicatorHeight.orElse(12.dp),
            barHeight = barHeight.orElse(BarHeight),
            contentPadding = contentPadding.orElse(12.dp),
            labelTextStyle = if (labelTextStyle == TextStyle.Default) MaterialTheme.typography.labelSmall else labelTextStyle,
            badgeTextStyle = if (badgeTextStyle == TextStyle.Default) MaterialTheme.typography.labelSmall else badgeTextStyle,
        )
    }

    /**
     * Builds a [BottomBarAnimationSpec]. The defaults reproduce the reference design's springy
     * indicator travel and a subtle icon pop; override any field to retune the motion.
     */
    fun animationSpec(
        indicatorSpec: androidx.compose.animation.core.AnimationSpec<Float> =
            DefaultAnimationSpecs.Indicator,
        iconColorSpec: androidx.compose.animation.core.AnimationSpec<Color> =
            DefaultAnimationSpecs.IconColor,
        iconScaleSpec: androidx.compose.animation.core.AnimationSpec<Float> =
            DefaultAnimationSpecs.IconScale,
    ): BottomBarAnimationSpec = BottomBarAnimationSpec(
        indicatorSpec = indicatorSpec,
        iconColorSpec = iconColorSpec,
        iconScaleSpec = iconScaleSpec,
    )
}

/** Returns the receiver if it is specified, otherwise [fallback]. */
private fun Color.orElse(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

/** Returns the receiver if it is specified, otherwise [fallback]. */
private fun Dp.orElse(fallback: Dp): Dp =
    if (this == Dp.Unspecified) fallback else this
