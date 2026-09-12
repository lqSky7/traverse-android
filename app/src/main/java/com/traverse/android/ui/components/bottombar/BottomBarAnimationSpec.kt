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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Bundles the animation specs used by [AnimatedBottomBar]. Obtain an instance from
 * [BottomBarDefaults.animationSpec].
 *
 * @property indicatorSpec drives the horizontal travel of the floating curved indicator as the
 *   selection changes.
 * @property iconColorSpec drives the tween between selected/unselected icon colors.
 * @property iconScaleSpec drives the subtle pop-scale of the icon as it becomes selected.
 */
@Immutable
data class BottomBarAnimationSpec(
    val indicatorSpec: AnimationSpec<Float>,
    val iconColorSpec: AnimationSpec<androidx.compose.ui.graphics.Color>,
    val iconScaleSpec: AnimationSpec<Float>,
)

/** Default, tuned animation specs matching the reference design's motion. */
internal object DefaultAnimationSpecs {
    val Indicator: AnimationSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val IconColor: AnimationSpec<androidx.compose.ui.graphics.Color> = tween(durationMillis = 250)

    val IconScale: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}
