/*
 * Adapted from AndroidLiquidGlass by kyant0.
 * Source: https://github.com/Kyant0/AndroidLiquidGlass
 * Licensed under the Apache License, Version 2.0.
 */

package com.traverse.android.ui.components.glass.backdrops

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.traverse.android.ui.components.glass.GlassBackdrop

@Stable
fun emptyBackdrop(): GlassBackdrop = EmptyGlassBackdrop

@Immutable
private object EmptyGlassBackdrop : GlassBackdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
    }
}
