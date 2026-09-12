package com.traverse.android.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.traverse.android.ui.theme.ColorPalette
import com.traverse.android.ui.theme.ColorPaletteManager
import com.traverse.android.ui.theme.hsvColor
import com.traverse.android.ui.theme.toHexString
import com.traverse.android.ui.theme.toHsv
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// This component is heavily inspired by https://github.com/AdelaideSky's work!

/**
 * 1:1 Kotlin port of the iOS `HuePicker` (`Views/HuePicker.swift`).
 *
 * Every constant, falloff formula, name table and harmony branch is copied verbatim from the
 * SwiftUI source so that the same finger position produces the same palette on both platforms.
 */

// iOS `Color(.systemBackground)` / `Color(.secondarySystemBackground)` for the always-dark app.
private val SystemBackground = Color(0xFF000000)
private val SecondarySystemBackground = Color(0xFF1C1C1E)

private const val COLUMNS = 13
private const val ROWS = 10
private const val DOT_SIZE_DP = 6f
private const val INFLUENCE_RADIUS_DP = 50f
private const val CURSOR_RADIUS_DP = 29f
private const val CURSOR_DIAMETER_DP = 58f
private const val EDGE_THRESHOLD_DP = 15f

/** iOS `sheetGridBackground` / `sheetGridBorder` corner radius. */
private const val GRID_CORNER_DP = 20f

/** iOS `HuePicker` presentation detent: `.fraction(0.72)`. */
private const val SHEET_HEIGHT_FRACTION = 0.72f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuePickerSheet(onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // MARK: - State (mirrors the `@State` block of the iOS view)
    var dragLocation by remember { mutableStateOf<Offset?>(null) }
    var currentColor by remember { mutableStateOf(hsvColor(0.4, 0.6, 0.9)) }
    var lastCrossedHorizontalGuide by remember { mutableStateOf(false) }
    var lastCrossedVerticalGuide by remember { mutableStateOf(false) }
    var wasNearEdge by remember { mutableStateOf(false) }
    var colorName by remember { mutableStateOf("Mint Split") }
    var currentYFraction by remember { mutableStateOf(0.5) }
    var gridSize by remember { mutableStateOf(IntSize.Zero) }

    val lightTick = HapticFeedbackType.TextHandleMove

    // iOS `.onAppear` seeds the name from the default `currentColor` (hue 0.4, centred vertically).
    LaunchedEffect(Unit) {
        colorName = "${dynamicColorName(0.4)} ${dynamicStyleName(0.5)}"
    }

    // MARK: - Animations
    // iOS animates every dot independently with `.spring(response: 0.2, dampingFraction: 0.3)`.
    // We reproduce the same curve with a single 0→1 "drag presence" value and blend each dot
    // between its resting and dragged appearance, which keeps the 130-dot grid cheap to draw.
    val dragPresence by animateFloatAsState(
        targetValue = if (dragLocation != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 1000f),
        label = "huePickerDragPresence"
    )

    val cursorOffset by animateOffsetAsState(
        targetValue = dragLocation ?: Offset.Zero,
        // iOS `.interactiveSpring(response: 0.08, dampingFraction: 0.9)`
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 6000f),
        label = "huePickerCursor"
    )

    val borderColor by animateColorAsState(
        targetValue = currentColor.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 300),
        label = "huePickerBorder"
    )

    val glowStrong by animateColorAsState(
        targetValue = currentColor.copy(alpha = 0.25f),
        animationSpec = tween(durationMillis = 400),
        label = "huePickerGlowStrong"
    )
    val glowSoft by animateColorAsState(
        targetValue = currentColor.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 400),
        label = "huePickerGlowSoft"
    )

    // MARK: - Drag handling (iOS `sheetDragGesture`)
    val onDragAt: (Offset) -> Unit = { raw ->
        if (gridSize.width > 0 && gridSize.height > 0) {
            val gridWidth = gridSize.width.toFloat()
            val gridHeight = gridSize.height.toFloat()
            val spacingY = gridHeight / ROWS

            // Inset by the cursor radius so the cursor stays visually within the grid.
            val cursorRadius = with(density) { CURSOR_RADIUS_DP.dp.toPx() }
            val clampedX = min(max(cursorRadius, raw.x), gridWidth - cursorRadius)
            val clampedY = min(max(cursorRadius, raw.y), gridHeight - cursorRadius)

            // Haptic when crossing the centre crosshair guides.
            val centerX = gridWidth / 2f
            val centerY = gridHeight / 2f
            val guideThreshold = spacingY * 0.5f
            val nearHorizontalGuide = abs(clampedY - centerY) < guideThreshold
            val nearVerticalGuide = abs(clampedX - centerX) < guideThreshold

            if (nearHorizontalGuide && !lastCrossedHorizontalGuide) {
                haptics.performHapticFeedback(lightTick)
            }
            if (nearVerticalGuide && !lastCrossedVerticalGuide) {
                haptics.performHapticFeedback(lightTick)
            }
            lastCrossedHorizontalGuide = nearHorizontalGuide
            lastCrossedVerticalGuide = nearVerticalGuide

            // Haptic when touching the edges.
            val edgeThreshold = with(density) { EDGE_THRESHOLD_DP.dp.toPx() }
            val nearEdge = clampedX < edgeThreshold || clampedX > gridWidth - edgeThreshold ||
                clampedY < edgeThreshold || clampedY > gridHeight - edgeThreshold
            if (nearEdge && !wasNearEdge) {
                haptics.performHapticFeedback(lightTick)
            }
            wasNearEdge = nearEdge

            dragLocation = Offset(clampedX, clampedY)

            val normalizedX = clampedX / gridWidth
            val normalizedY = clampedY / gridHeight
            currentYFraction = normalizedY.toDouble()
            currentColor = interpolateColor(
                x = normalizedX.toDouble(),
                y = normalizedY.toDouble(),
                vibrancy = ColorPaletteManager.vibrancy
            )

            val combined =
                "${dynamicColorName(normalizedX.toDouble())} " +
                    dynamicStyleName(normalizedY.toDouble())
            if (combined != colorName) {
                haptics.performHapticFeedback(lightTick)
                colorName = combined
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SystemBackground,
        contentColor = Color.White,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
        ) {
            // iOS `sheetBackground` — opaque base plus a top-leading radial glow.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowStrong, glowSoft, Color.Transparent),
                            center = Offset.Zero,
                            radius = with(density) { 400.dp.toPx() }
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // MARK: sheetMoodTextView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 15.dp)
                        .height(60.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) {
                                append("Pick a hue, and we'll make\na ")
                            }
                            withStyle(SpanStyle(color = currentColor)) {
                                append(colorName)
                            }
                            withStyle(SpanStyle(color = Color.White)) {
                                append(" palette for you")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // MARK: sheetDotGridView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 15.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(GRID_CORNER_DP.dp))
                        .background(SecondarySystemBackground.copy(alpha = 0.6f))
                        .border(1.dp, borderColor, RoundedCornerShape(GRID_CORNER_DP.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .onSizeChanged { gridSize = it }
                            .pointerInput(gridSize) {
                                awaitEachGesture {
                                    // `DragGesture(minimumDistance: 0)` — react on touch-down.
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    onDragAt(down.position)

                                    var active = true
                                    while (active) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            active = false
                                        } else {
                                            onDragAt(change.position)
                                            change.consume()
                                        }
                                    }

                                    // iOS `.onEnded` — success haptic, save, dismiss.
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    ColorPaletteManager.saveCustomPalette(
                                        ColorPalette(
                                            id = ColorPalette.HUE_PICKER_PALETTE_ID,
                                            name = colorName,
                                            colors = generatePalette(currentColor, currentYFraction)
                                        )
                                    )
                                    onDismiss()
                                }
                            }
                    ) {
                        val vibrancy = ColorPaletteManager.vibrancy
                        val minSat = 0.10 + vibrancy * 0.35
                        val maxSat = 0.30 + vibrancy * 0.65
                        val brightness = 0.98 - vibrancy * 0.08

                        val spacingX = size.width / COLUMNS
                        val spacingY = size.height / ROWS
                        val dotPx = DOT_SIZE_DP.dp.toPx()
                        val influencePx = INFLUENCE_RADIUS_DP.dp.toPx()

                        val hasDrag = dragLocation != null
                        val dragX = dragLocation?.x ?: 0f
                        val dragY = dragLocation?.y ?: 0f

                        for (row in 0 until ROWS) {
                            for (col in 0 until COLUMNS) {
                                val cx = spacingX * (col + 0.5f)
                                val cy = spacingY * (row + 0.5f)
                                // Single centre guide dot.
                                val isGuideDot = row == ROWS / 2 && col == COLUMNS / 2
                                val restScale = if (isGuideDot) 0.5f else 0.3f
                                val restOpacity = if (isGuideDot) 0.5f else 0.3f

                                // iOS `DotView.distance` — `.infinity` when not dragging.
                                val distance = if (hasDrag) {
                                    val dx = cx - dragX
                                    val dy = cy - dragY
                                    sqrt(dx * dx + dy * dy)
                                } else {
                                    Float.MAX_VALUE
                                }
                                val normalized = distance / influencePx

                                val targetScale = max(restScale, min(1.5f, 1.5f - normalized * 0.8f))
                                val targetOpacity = max(restOpacity, min(1f, 1f - normalized * 0.7f))

                                val scale = restScale + (targetScale - restScale) * dragPresence
                                val opacity = restOpacity + (targetOpacity - restOpacity) * dragPresence

                                // iOS `DotView.dotColor`.
                                val dotColor = hsvColor(
                                    hue = (cx / size.width).toDouble(),
                                    saturation = (minSat + (cy / size.height) * (maxSat - minSat)),
                                    brightness = brightness
                                )

                                val drawnColor = lerp(Color.Gray, dotColor, dragPresence)

                                val baseDiameter = if (isGuideDot) dotPx * 1.2f else dotPx
                                drawCircle(
                                    color = drawnColor.copy(alpha = drawnColor.alpha * opacity),
                                    radius = baseDiameter / 2f * scale,
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                        // iOS `sheetCursorView` — 58pt circle at 20% of the current colour.
                        if (hasDrag) {
                            drawCircle(
                                color = currentColor.copy(alpha = 0.2f),
                                radius = CURSOR_DIAMETER_DP.dp.toPx() / 2f,
                                center = cursorOffset
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - iOS `interpolateColor(x:y:)`

private fun interpolateColor(x: Double, y: Double, vibrancy: Double): Color {
    val minSat = 0.10 + (vibrancy * 0.35)
    val maxSat = 0.30 + (vibrancy * 0.65)
    val saturation = minSat + (y * (maxSat - minSat))
    val brightness = 0.98 - (vibrancy * 0.08)
    return hsvColor(x, saturation, brightness)
}

// MARK: - iOS `getDynamicColorName(hue:)` (27 hue bands)

private fun dynamicColorName(hue: Double): String = when {
    hue < 0.03 -> "Scarlet"
    hue < 0.06 -> "Coral"
    hue < 0.09 -> "Orange"
    hue < 0.12 -> "Peach"
    hue < 0.15 -> "Amber"
    hue < 0.18 -> "Gold"
    hue < 0.21 -> "Yellow"
    hue < 0.25 -> "Lime"
    hue < 0.29 -> "Olive"
    hue < 0.34 -> "Green"
    hue < 0.38 -> "Emerald"
    hue < 0.42 -> "Mint"
    hue < 0.46 -> "Teal"
    hue < 0.50 -> "Cyan"
    hue < 0.54 -> "Turquoise"
    hue < 0.58 -> "Sky Blue"
    hue < 0.62 -> "Azure"
    hue < 0.66 -> "Cobalt"
    hue < 0.70 -> "Blue"
    hue < 0.74 -> "Sapphire"
    hue < 0.78 -> "Indigo"
    hue < 0.82 -> "Lavender"
    hue < 0.86 -> "Purple"
    hue < 0.90 -> "Violet"
    hue < 0.94 -> "Plum"
    hue < 0.97 -> "Rose"
    else -> "Crimson"
}

// MARK: - iOS `getDynamicStyleName(y:)` (5 harmony styles)

private fun dynamicStyleName(y: Double): String = when {
    y < 0.20 -> "Mono"
    y < 0.40 -> "Analog"
    y < 0.60 -> "Split"
    y < 0.80 -> "Triad"
    else -> "Complement"
}

// MARK: - iOS `generatePalette(from:y:)`

private fun generatePalette(baseColor: Color, y: Double): List<String> {
    val hsv = baseColor.toHsv()
    val hue = hsv[0].toDouble()
    val saturation = hsv[1].toDouble()
    val brightness = hsv[2].toDouble()

    val colors: List<Color> = when {
        // Monochromatic
        y < 0.20 -> listOf(
            baseColor,
            hsvColor(hue, max(saturation - 0.15, 0.15), min(brightness + 0.05, 1.0)),
            hsvColor(hue, max(saturation - 0.3, 0.1), min(brightness + 0.1, 1.0)),
            hsvColor(hue, min(saturation + 0.15, 0.9), max(brightness - 0.1, 0.4)),
            hsvColor(hue, min(saturation + 0.3, 1.0), max(brightness - 0.2, 0.3))
        )
        // Analogous
        y < 0.40 -> listOf(
            baseColor,
            hsvColor((hue + 0.083) % 1.0, saturation * 0.9, min(brightness * 1.1, 1.0)),
            hsvColor((hue + 0.917) % 1.0, saturation * 0.9, min(brightness * 1.1, 1.0)),
            hsvColor((hue + 0.041) % 1.0, saturation * 0.95, min(brightness * 1.05, 1.0)),
            hsvColor((hue + 0.959) % 1.0, saturation * 0.95, min(brightness * 1.05, 1.0))
        )
        // Split Complementary
        y < 0.60 -> listOf(
            baseColor,
            hsvColor((hue + 0.083) % 1.0, saturation, brightness),
            hsvColor((hue + 0.416) % 1.0, saturation * 0.8, min(brightness * 1.1, 1.0)),
            hsvColor((hue + 0.583) % 1.0, saturation * 0.8, min(brightness * 1.1, 1.0)),
            hsvColor((hue - 0.083 + 1.0) % 1.0, saturation, brightness)
        )
        // Triadic
        y < 0.80 -> listOf(
            baseColor,
            hsvColor((hue + 0.333) % 1.0, saturation * 0.9, brightness),
            hsvColor((hue + 0.667) % 1.0, saturation * 0.9, brightness),
            hsvColor(hue, saturation * 0.6, min(brightness * 1.15, 1.0)),
            hsvColor((hue + 0.333) % 1.0, saturation * 1.1, max(brightness * 0.85, 0.4))
        )
        // Complementary
        else -> listOf(
            baseColor,
            hsvColor(hue, saturation * 0.7, min(brightness * 1.1, 1.0)),
            hsvColor((hue + 0.5) % 1.0, saturation, brightness),
            hsvColor((hue + 0.5) % 1.0, saturation * 0.7, min(brightness * 1.1, 1.0)),
            hsvColor((hue + 0.5) % 1.0, min(saturation * 1.2, 1.0), max(brightness * 0.8, 0.4))
        )
    }

    return colors.map { it.toHexString() }
}
