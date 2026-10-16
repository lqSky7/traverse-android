/*
 * Adapted from AndroidLiquidGlass by kyant0.
 * Source: https://github.com/Kyant0/AndroidLiquidGlass
 * Licensed under the Apache License, Version 2.0.
 */

package com.traverse.android.ui.components.glass.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.traverse.android.ui.components.glass.recordLayer

fun Modifier.layerGlassBackdrop(backdrop: LayerGlassBackdrop): Modifier =
    this then LayerGlassBackdropElement(backdrop)

private class LayerGlassBackdropElement(
    val backdrop: LayerGlassBackdrop
) : ModifierNodeElement<LayerGlassBackdropNode>() {

    override fun create(): LayerGlassBackdropNode {
        return LayerGlassBackdropNode(backdrop)
    }

    override fun update(node: LayerGlassBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerGlassBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerGlassBackdropElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class LayerGlassBackdropNode(
    var backdrop: LayerGlassBackdrop
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw() {
        drawContent()
        recordLayer(this@LayerGlassBackdropNode, backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.layerCoordinates = coordinates
        }
    }

    override fun onDetach() {
        backdrop.layerCoordinates = null
    }
}
