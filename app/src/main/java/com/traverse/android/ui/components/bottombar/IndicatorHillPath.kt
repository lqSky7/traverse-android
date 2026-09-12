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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

/**
 * Builds the outline of the floating indicator "hill" — the soft, symmetric bump that rises from the
 * bar's bottom edge and slides beneath the selected item. The hill is centred at [centerX] along the
 * bottom edge.
 *
 * The bump is traced with two symmetric cubic Béziers so its shoulders blend tangentially into the
 * flat bottom edge — this is what gives the reference design its gentle, liquid rise rather than a
 * hard triangular tooth. The path is closed along the bottom edge so it can be filled solid.
 *
 * @param size the size of the bar container in pixels; [Size.height] is the bottom edge the hill
 *   rests on.
 * @param centerX the horizontal centre of the hill, in pixels.
 * @param halfWidth half the width of the hill's base along the bottom edge, in pixels.
 * @param height how far the hill's peak rises above the bottom edge, in pixels.
 * @return a freshly built, closed [Path] describing the hill, ready to fill with the indicator color.
 */
internal fun buildIndicatorHillPath(
    size: Size,
    centerX: Float,
    halfWidth: Float,
    height: Float,
): Path {
    val bottom = size.height
    val top = bottom - height

    val leftBase = centerX - halfWidth
    val rightBase = centerX + halfWidth

    // Control-point spread that shapes how gently the wall leaves the bottom edge and reaches the
    // rounded peak. Tuned to match the reference "hill" feel: broad, soft shoulders.
    val shoulder = halfWidth * 0.55f
    val crown = halfWidth * 0.35f

    return Path().apply {
        // Start at the left base, on the bottom edge.
        moveTo(leftBase, bottom)

        // Rise from the bottom edge up to the crown.
        cubicTo(
            leftBase + shoulder, bottom,
            centerX - crown, top,
            centerX, top,
        )
        // Descend from the crown back down to the bottom edge (mirror of the rise).
        cubicTo(
            centerX + crown, top,
            rightBase - shoulder, bottom,
            rightBase, bottom,
        )

        // Close along the bottom edge.
        close()
    }
}
