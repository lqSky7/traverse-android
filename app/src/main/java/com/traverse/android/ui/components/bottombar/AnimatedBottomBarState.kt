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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Hoisted state for an [AnimatedBottomBar].
 *
 * The bar is a fully controlled component: the source of truth for the selected index is whatever
 * you pass to `AnimatedBottomBar(selectedIndex = …)`. This state holder additionally survives
 * configuration changes and process death via [Saver].
 */
@Stable
class AnimatedBottomBarState internal constructor(
    initialSelectedIndex: Int,
) {
    /** The index most recently selected via user interaction. */
    var selectedIndex: Int by mutableIntStateOf(initialSelectedIndex)

    internal companion object {
        val Saver: Saver<AnimatedBottomBarState, Int> = Saver(
            save = { it.selectedIndex },
            restore = { AnimatedBottomBarState(initialSelectedIndex = it) },
        )
    }
}

/**
 * Creates and remembers an [AnimatedBottomBarState] that survives recomposition, configuration
 * changes, and process death.
 *
 * @param initialSelectedIndex the index selected on first composition.
 */
@Composable
fun rememberAnimatedBottomBarState(
    initialSelectedIndex: Int = 0,
): AnimatedBottomBarState = rememberSaveable(saver = AnimatedBottomBarState.Saver) {
    AnimatedBottomBarState(initialSelectedIndex = initialSelectedIndex)
}
