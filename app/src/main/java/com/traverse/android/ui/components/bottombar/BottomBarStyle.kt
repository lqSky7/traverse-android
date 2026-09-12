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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual styling for an [AnimatedBottomBar]. Obtain an instance from [BottomBarDefaults.style],
 * overriding only the properties you care about.
 *
 * @property containerColor background color of the bar's rounded container.
 * @property indicatorColor color of the indicator "hill" that rises from the bottom edge beneath the
 *   selected item.
 * @property selectedIconColor tint of the selected item's icon.
 * @property unselectedIconColor tint of unselected items' icons.
 * @property disabledIconColor tint of a disabled item's icon.
 * @property badgeColor background color of a [Badge].
 * @property badgeContentColor color of the text inside a [Badge.Count].
 * @property containerShapeRadius corner radius of the bar container.
 * @property iconSize size of each item icon.
 * @property indicatorSize width of the indicator hill's base along the bottom edge.
 * @property indicatorHeight how far the indicator hill's peak rises above the bar's bottom edge.
 * @property barHeight height of the bar container.
 * @property contentPadding horizontal padding between the container edge and the first/last item.
 */
@Immutable
data class BottomBarStyle internal constructor(
    val containerColor: Color,
    val indicatorColor: Color,
    val selectedIconColor: Color,
    val unselectedIconColor: Color,
    val disabledIconColor: Color,
    val badgeColor: Color,
    val badgeContentColor: Color,
    val containerShapeRadius: Dp,
    val iconSize: Dp,
    val indicatorSize: Dp,
    val indicatorHeight: Dp,
    val barHeight: Dp,
    val contentPadding: Dp,
    val labelTextStyle: TextStyle,
    val badgeTextStyle: TextStyle,
) {
    companion object {
        /** Sentinel used by [BottomBarDefaults.style] to detect unspecified dimensions. */
        internal val Unspecified: Dp = Dp.Unspecified
    }
}
