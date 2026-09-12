package com.traverse.android.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/**
 * 1:1 Kotlin port of the iOS `ColorPalette` model (`Models/ColorPalette.swift`).
 *
 * The colour hexes are identical to iOS, and the palette order matches
 * `ColorPalette.allPalettes`, so palette id N means the same thing on both platforms.
 */
data class ColorPalette(
    val id: Int,
    val name: String,
    val colors: List<String>
) {
    /** iOS `swiftUIColors` — raw, *unadjusted* hex colours. */
    val swiftUIColors: List<Color> get() = colors.map { parseHexColor(it) }

    /** iOS `primary` — `Color(hex: colors[0])`. */
    val primary: Color get() = parseHexColor(colors.first())

    /** iOS `secondary` — `Color(hex: colors[safe: 1] ?? colors[0])`. */
    val secondary: Color get() = parseHexColor(colors.getOrElse(1) { colors.first() })

    /** iOS `ColorPaletteManager.color(at:)` — index is cycled, never out of bounds. */
    fun colorAt(index: Int): Color {
        val list = swiftUIColors
        return list[index.mod(list.size)]
    }

    companion object {
        /** iOS `ImportPaletteView` / `importPalette(from:)` custom palette id. */
        const val CUSTOM_PALETTE_ID = 999

        /** iOS `HuePicker.saveSelection()` palette id. */
        const val HUE_PICKER_PALETTE_ID = 1000

        /** Exactly mirrors `ColorPalette.allPalettes` on iOS. */
        val allPalettes: List<ColorPalette> = listOf(
            ColorPalette(
                id = 0,
                name = "Monochrome",
                colors = listOf("8E8E93", "636366", "AEAEB2", "C7C7CC", "D1D1D6")
            ),
            ColorPalette(
                id = 1,
                name = "Ocean Breeze",
                colors = listOf("0077B6", "00B4D8", "90E0EF", "CAF0F8", "48CAE4")
            ),
            ColorPalette(
                id = 2,
                name = "Sunset Glow",
                colors = listOf("FF6B6B", "FFA06D", "FFD93D", "FF8E53", "FF5E5B")
            ),
            ColorPalette(
                id = 3,
                name = "Forest Zen",
                colors = listOf("2D6A4F", "40916C", "52B788", "74C69D", "95D5B2")
            ),
            ColorPalette(
                id = 4,
                name = "Candy Dream",
                colors = listOf("CDB4DB", "FFC8DD", "FFAFCC", "BDE0FE", "A2D2FF")
            ),
            ColorPalette(
                id = 5,
                name = "Neon Nights",
                colors = listOf("FF00FF", "00FFFF", "FF6EC7", "7DF9FF", "39FF14")
            ),
            ColorPalette(
                id = 6,
                name = "Lavender Fields",
                colors = listOf("E0BBE4", "957DAD", "D291BC", "FEC8D8", "FFDFD3")
            )
        )

        val default: ColorPalette get() = allPalettes[0]
    }
}

/**
 * Mirrors the iOS `Color` hex initialiser: accepts 3, 6 or 8 hex digits
 * (RGB / RRGGBB / AARRGGBB) and ignores a leading `#`.
 */
fun parseHexColor(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    return try {
        when (cleaned.length) {
            3 -> {
                val r = cleaned[0].digitToInt(16) * 17
                val g = cleaned[1].digitToInt(16) * 17
                val b = cleaned[2].digitToInt(16) * 17
                Color(red = r, green = g, blue = b)
            }

            6 -> Color(0xFF000000L or cleaned.toLong(16))

            8 -> Color(cleaned.toLong(16))

            else -> Color.Black
        }
    } catch (_: NumberFormatException) {
        Color.Black
    }
}

/**
 * Mirrors the iOS `Color.toHex()` extension: `String(format: "%02X%02X%02X", r, g, b)`
 * — six uppercase hex digits, no `#` and no alpha channel.
 */
fun Color.toHexString(): String {
    val argb = this.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format(Locale.US, "%02X%02X%02X", r, g, b)
}

/**
 * Mirrors `UIColor.getHue(_:saturation:brightness:alpha:)` — returns `[hue, saturation, brightness]`
 * with every component normalised to `0f..1f`.
 */
fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return hsv
}

/**
 * Mirrors `Color(hue:saturation:brightness:)`. Compose wants the hue in degrees, so the
 * `0..1` SwiftUI hue is scaled by 360. Every component is clamped, matching `UIColor`'s behaviour
 * for the slightly out-of-range values `generatePalette` produces (e.g. `saturation * 1.1`).
 */
fun hsvColor(hue: Double, saturation: Double, brightness: Double): Color = Color.hsv(
    hue = (hue.coerceIn(0.0, 1.0) * 360.0).toFloat(),
    saturation = saturation.coerceIn(0.0, 1.0).toFloat(),
    value = brightness.coerceIn(0.0, 1.0).toFloat()
)

/**
 * 1:1 Kotlin port of the iOS `ColorPaletteManager` singleton.
 *
 * `selectedPalette` is backed by Compose snapshot state, so any composable that reads
 * `ColorPaletteManager.selectedPalette` (directly or through [rememberPalette]) will
 * recompose the moment the palette changes — matching how `@ObservedObject` behaves on iOS.
 *
 * Note: iOS `adjustedForDarkMode()` is a no-op in practice because the `isDarkMode`
 * UserDefaults key is never written, so `primary` is simply `colors[0]`.
 */
object ColorPaletteManager {

    private const val PREFS_NAME = "traverse_color_palette"
    private const val KEY_SELECTED_ID = "selectedColorPaletteID"
    private const val KEY_CUSTOM = "customColorPalette"
    private const val KEY_VIBRANCY = "huePickerVibrancy"

    /** iOS `ColorPaletteManager.vibrancy` default. */
    const val DEFAULT_VIBRANCY = 0.15

    private var prefs: SharedPreferences? = null

    var selectedPalette by mutableStateOf(ColorPalette.default)
        private set

    var customPalette by mutableStateOf<ColorPalette?>(null)
        private set

    private var _vibrancy by mutableStateOf(DEFAULT_VIBRANCY)

    /**
     * iOS `@Published var vibrancy` — drives the saturation range and brightness of the
     * `HuePicker` dot grid. Snapshot state so the picker recomposes when it changes, and
     * persisted under the same `huePickerVibrancy` key iOS uses.
     */
    var vibrancy: Double
        get() = _vibrancy
        set(value) {
            _vibrancy = value
            prefs?.edit()?.putFloat(KEY_VIBRANCY, value.toFloat())?.apply()
        }

    /** iOS `allAvailablePalettes` — the 7 built-ins plus a custom palette if one exists. */
    val allAvailablePalettes: List<ColorPalette>
        get() = ColorPalette.allPalettes + listOfNotNull(customPalette)

    /** Safe to call repeatedly; the first call wins. */
    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store

        val loadedCustom = store.getString(KEY_CUSTOM, null)?.let(::decodeCustomPalette)
        customPalette = loadedCustom

        val savedId = store.getInt(KEY_SELECTED_ID, ColorPalette.default.id)
        selectedPalette = when {
            loadedCustom != null && loadedCustom.id == savedId -> loadedCustom
            else -> ColorPalette.allPalettes.firstOrNull { it.id == savedId } ?: ColorPalette.default
        }

        _vibrancy = store.getFloat(KEY_VIBRANCY, DEFAULT_VIBRANCY.toFloat()).toDouble()
    }

    /** iOS `selectPalette(_:)`. */
    fun selectPalette(palette: ColorPalette) {
        selectedPalette = palette
        prefs?.edit()?.putInt(KEY_SELECTED_ID, palette.id)?.apply()
    }

    /**
     * iOS `importPalette(from:)` — accepts a coolors.co URL or a list of hex codes
     * separated by commas / whitespace / newlines. Requires at least 3 valid colours.
     */
    fun importPalette(input: String): Boolean {
        val colors = parsePaletteInput(input) ?: return false
        val palette = ColorPalette(
            id = ColorPalette.CUSTOM_PALETTE_ID,
            name = "Custom",
            colors = colors
        )
        customPalette = palette
        prefs?.edit()?.putString(KEY_CUSTOM, encodeCustomPalette(palette))?.apply()
        selectPalette(palette)
        return true
    }

    /**
     * iOS `HuePicker.saveSelection()` — assigns the generated palette to **both**
     * `customPalette` and `selectedPalette` and persists it.
     */
    fun saveCustomPalette(palette: ColorPalette) {
        customPalette = palette
        prefs?.edit()?.putString(KEY_CUSTOM, encodeCustomPalette(palette))?.apply()
        selectPalette(palette)
    }

    /** Clears the stored custom palette and falls back to the default built-in palette. */
    fun clearCustomPalette() {
        customPalette = null
        prefs?.edit()?.remove(KEY_CUSTOM)?.apply()
        // Both custom ids (999 import / 1000 hue picker) live in the same single slot, so
        // reset whenever the selection is no longer one of the built-ins.
        if (ColorPalette.allPalettes.none { it.id == selectedPalette.id }) {
            selectPalette(ColorPalette.default)
        }
    }

    /** Convenience wrapper for `ColorPaletteManager.selectedPalette.colorAt(index)`. */
    fun colorAt(index: Int): Color = selectedPalette.colorAt(index)

    // MARK: - Input parsing (mirrors parsePaletteInput / parseCoolorsURL / parseSCSSFormat)

    private fun parsePaletteInput(input: String): List<String>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.contains("coolors.co", ignoreCase = true) -> parseCoolorsUrl(trimmed)
            trimmed.contains("$") && trimmed.contains(":") -> parseScssFormat(trimmed)
            else -> parseHexList(trimmed)
        }
    }

    private fun parseCoolorsUrl(url: String): List<String>? {
        val lastComponent = url.substringAfterLast('/')
        val valid = lastComponent.split('-')
            .map { it.replace("#", "").trim() }
            .filter { it.length == 6 || it.length == 8 }
        return valid.takeIf { it.size >= 3 }
    }

    private fun parseScssFormat(scss: String): List<String>? {
        val colors = scss.lineSequence().mapNotNull { line ->
            if (!line.contains(":") || !line.contains("#")) return@mapNotNull null
            val colorPart = line.substringAfter(':').trim()
            val cleaned = colorPart
                .replace("#", "")
                .replace(";", "")
                .removePrefix("ff")
                .trim()
            cleaned.takeIf { it.length == 6 || it.length == 8 }
        }.toList()
        return colors.takeIf { it.size >= 3 }
    }

    private fun parseHexList(input: String): List<String>? {
        val valid = input.split(',', ' ', '\n', '\t')
            .map { it.replace("#", "").trim() }
            .filter { it.length == 6 || it.length == 8 }
        return valid.takeIf { it.size >= 3 }
    }

    // MARK: - Custom palette persistence (delimiter based, no JSON dependency)

    private const val FIELD_SEPARATOR = "|"
    private const val COLOR_SEPARATOR = ","

    private fun encodeCustomPalette(palette: ColorPalette): String =
        "${palette.id}$FIELD_SEPARATOR${palette.name}$FIELD_SEPARATOR" +
            palette.colors.joinToString(COLOR_SEPARATOR)

    private fun decodeCustomPalette(raw: String): ColorPalette? {
        val parts = raw.split(FIELD_SEPARATOR)
        if (parts.size < 3) return null
        val id = parts[0].toIntOrNull() ?: return null
        val colors = parts[2].split(COLOR_SEPARATOR).filter { it.isNotBlank() }
        if (colors.isEmpty()) return null
        return ColorPalette(id = id, name = parts[1], colors = colors)
    }
}

/**
 * Reads the currently selected palette inside a composable. Because [ColorPaletteManager.selectedPalette]
 * is snapshot state, the calling composable recomposes whenever the palette changes.
 */
@androidx.compose.runtime.Composable
fun rememberPalette(): ColorPalette = ColorPaletteManager.selectedPalette

// MARK: - Non-composable accessors
//
// `rememberPalette()` can only be called from a composable, which is awkward inside the many
// plain helper functions that resolve an accent colour (e.g. `difficultyColor(...)`). These
// accessors read the very same snapshot state, so a read that happens during composition is
// still tracked and the caller still recomposes when the palette changes.

/** The currently selected palette. */
val currentPalette: ColorPalette
    get() = ColorPaletteManager.selectedPalette

/** `currentPalette.colorAt(index)` — the iOS `paletteManager.color(at:)` equivalent. */
fun paletteColorAt(index: Int): Color = ColorPaletteManager.selectedPalette.colorAt(index)

/** `currentPalette.primary` — the iOS `selectedPalette.primary` equivalent. */
val palettePrimary: Color
    get() = ColorPaletteManager.selectedPalette.primary
