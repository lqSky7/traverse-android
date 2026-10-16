/*
 * Adapted from AndroidLiquidGlass by kyant0.
 * Source: https://github.com/Kyant0/AndroidLiquidGlass
 * Licensed under the Apache License, Version 2.0.
 */

package com.traverse.android.ui.components.glass

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface GlassBackdrop {

    val isCoordinatesDependent: Boolean

    fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
