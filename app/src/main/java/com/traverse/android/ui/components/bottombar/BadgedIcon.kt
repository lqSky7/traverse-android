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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Badge as M3Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Renders an item's icon with an optional [Badge] overlaid in the top-right, mirroring Material 3's
 * badged-box positioning but sized for this bar. The badge is decorative for a11y — its meaning is
 * folded into the item's content description upstream (see [Badge.accessibilitySuffix]).
 */
@Composable
internal fun BadgedIcon(
    item: BottomBarItem,
    selected: Boolean,
    tint: Color,
    style: BottomBarStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.wrapContentSize()) {

        // Evaluate the sealed interface and use the correct Icon overload
        when (val iconSource = item.iconFor(selected)) {
            is IconSource.Vector -> {
                Icon(
                    imageVector = iconSource.imageVector,
                    contentDescription = null, // Handled upstream
                    tint = tint,
                    modifier = Modifier.size(style.iconSize),
                )
            }
            is IconSource.DrawableResource -> {
                Icon(
                    painter = painterResource(id = iconSource.resId),
                    contentDescription = null, // Handled upstream
                    tint = tint,
                    modifier = Modifier.size(style.iconSize),
                )
            }
        }

        val badge = item.badge
        if (badge != null && badge.isVisible()) {
            M3Badge(
                containerColor = style.badgeColor,
                contentColor = style.badgeContentColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
            ) {
                badge.displayText()?.let { Text(it) }
            }
        }
    }
}
