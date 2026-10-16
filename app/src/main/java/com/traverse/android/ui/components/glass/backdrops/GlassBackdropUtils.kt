/*
 * Adapted from AndroidLiquidGlass by kyant0.
 * Source: https://github.com/Kyant0/AndroidLiquidGlass
 * Licensed under the Apache License, Version 2.0.
 */

package com.traverse.android.ui.components.glass.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.traverse.android.ui.components.glass.GlassBackdrop

@Composable
fun rememberGlassBackdrop(
    backdrop: GlassBackdrop,
    onDraw: DrawScope.(drawGlassBackdrop: DrawScope.() -> Unit) -> Unit
): GlassBackdrop {
    return remember(backdrop, onDraw) {
        Backdrop(backdrop, onDraw)
    }
}

@Immutable
private class Backdrop(
    val backdrop: GlassBackdrop,
    val onDraw: DrawScope.(drawGlassBackdrop: DrawScope.() -> Unit) -> Unit
) : GlassBackdrop {

    override val isCoordinatesDependent: Boolean = backdrop.isCoordinatesDependent

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw { with(backdrop) { drawGlassBackdrop(density, coordinates, layerBlock) } }
    }
}
