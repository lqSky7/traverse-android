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
fun rememberCombinedGlassBackdrop(
    backdrop1: GlassBackdrop,
    backdrop2: GlassBackdrop
): GlassBackdrop {
    return remember(backdrop1, backdrop2) {
        Combined2Backdrops(backdrop1, backdrop2)
    }
}

@Composable
fun rememberCombinedGlassBackdrop(
    backdrop1: GlassBackdrop,
    backdrop2: GlassBackdrop,
    backdrop3: GlassBackdrop
): GlassBackdrop {
    return remember(backdrop1, backdrop2, backdrop3) {
        Combined3Backdrops(backdrop1, backdrop2, backdrop3)
    }
}

@Composable
fun rememberCombinedGlassBackdrop(vararg backdrops: GlassBackdrop): GlassBackdrop {
    return remember(*backdrops) {
        CombinedBackdrops(*backdrops)
    }
}

@Immutable
private class Combined2Backdrops(
    val backdrop1: GlassBackdrop,
    val backdrop2: GlassBackdrop
) : GlassBackdrop {

    override val isCoordinatesDependent: Boolean =
        backdrop1.isCoordinatesDependent || backdrop2.isCoordinatesDependent

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        with(backdrop1) { drawGlassBackdrop(density, coordinates, layerBlock) }
        with(backdrop2) { drawGlassBackdrop(density, coordinates, layerBlock) }
    }
}

@Immutable
private class Combined3Backdrops(
    val backdrop1: GlassBackdrop,
    val backdrop2: GlassBackdrop,
    val backdrop3: GlassBackdrop
) : GlassBackdrop {

    override val isCoordinatesDependent: Boolean =
        backdrop1.isCoordinatesDependent ||
                backdrop2.isCoordinatesDependent ||
                backdrop3.isCoordinatesDependent

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        with(backdrop1) { drawGlassBackdrop(density, coordinates, layerBlock) }
        with(backdrop2) { drawGlassBackdrop(density, coordinates, layerBlock) }
        with(backdrop3) { drawGlassBackdrop(density, coordinates, layerBlock) }
    }
}

@Immutable
private class CombinedBackdrops(
    vararg val backdrops: GlassBackdrop
) : GlassBackdrop {

    override val isCoordinatesDependent: Boolean =
        backdrops.any { it.isCoordinatesDependent }

    override fun DrawScope.drawGlassBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        backdrops.forEach { backdrop ->
            with(backdrop) { drawGlassBackdrop(density, coordinates, layerBlock) }
        }
    }
}
