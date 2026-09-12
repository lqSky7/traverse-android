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

/**
 * Represents a single destination displayed in an [AnimatedBottomBar].
 *
 * @property icon The icon displayed for this destination. If [selectedIcon] is `null`,
 * the same icon is used for both the selected and unselected states.
 * @property label A localized, human-readable label describing this destination.
 * @property selectedIcon An optional icon displayed when this destination is selected,
 * such as a filled version of an outlined icon. If `null`, [icon] is reused.
 * @property badge An optional badge displayed over the icon, such as an unread count.
 * @property enabled Controls whether this destination can be selected.
 */
@Immutable
data class BottomBarItem(
    val icon: IconSource,
    val label: String,
    val selectedIcon: IconSource? = null,
    val badge: Badge? = null,
    val enabled: Boolean = true,
) {
    /** The icon to draw for the given [selected] state, honouring [selectedIcon] when present. */
    internal fun iconFor(selected: Boolean): IconSource =
        if (selected) selectedIcon ?: icon else icon
}

/**
 * An optional badge rendered on an item's icon.
 *
 * Use [Badge.Dot] for a simple presence indicator, or [Badge.Count] to show a number (values above
 * [Badge.Count.max] are rendered as "max+").
 */
@Immutable
sealed interface Badge {

    /** A small dot with no text — signals "something new" without a count. */
    @Immutable
    data object Dot : Badge

    /**
     * A numeric badge.
     *
     * @property value the number to display. Non-positive values hide the badge.
     * @property max the largest number shown verbatim; larger values render as "[max]+".
     */
    @Immutable
    data class Count(
        val value: Int,
        val max: Int = 99,
    ) : Badge
}

/** Whether this badge should be drawn at all. */
internal fun Badge.isVisible(): Boolean = when (this) {
    is Badge.Dot -> true
    is Badge.Count -> value > 0
}

/** The text to render inside the badge, or `null` for a text-less [Badge.Dot]. */
internal fun Badge.displayText(): String? = when (this) {
    is Badge.Dot -> null
    is Badge.Count -> if (value > max) "$max+" else value.toString()
}

/** A short a11y suffix describing the badge, folded into an item's content description. */
internal fun Badge.accessibilitySuffix(): String = when (this) {
    is Badge.Dot -> "new notification"
    is Badge.Count -> if (value > max) "more than $max notifications" else "$value notifications"
}
