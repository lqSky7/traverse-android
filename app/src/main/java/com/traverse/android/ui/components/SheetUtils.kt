package com.traverse.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * NestedScrollConnection that prevents bottom sheets from glitching, jittering, or
 * over-dragging upwards when pulled up beyond their expanded limit.
 *
 * When content is at the top/expanded state and the user pulls or flings up (available.y < 0),
 * this connection intercepts and consumes the upward delta so it doesn't propagate into
 * invalid negative SheetState drag ranges.
 */
@Composable
fun rememberSheetOverscrollClamper(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When dragging up beyond the top/expanded limit (available.y < 0), consume it
                return if (available.y < 0f) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // When flinging up beyond top/expanded limit, consume upward velocity
                return if (available.y < 0f) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }
}
