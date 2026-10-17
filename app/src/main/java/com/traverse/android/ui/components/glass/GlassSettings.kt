package com.traverse.android.ui.components.glass

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide glass preferences.
 *
 * Performance mode is the escape hatch for older or slower phones: it turns
 * every glass surface into an opaque one and drops the blur, the refraction
 * shader and the per-frame layer captures. It is also what a device gets
 * automatically when the platform cannot blur at all — see [isLiquidGlassEnabled].
 *
 * Held in snapshot state so flipping the switch redraws the whole app live,
 * and persisted so it survives a restart.
 */
object GlassSettings {

    private const val PREFS = "traverse_ui_prefs"
    private const val KEY_PERFORMANCE = "performance_mode"

    /** When true, glass is replaced by its opaque equivalent everywhere. */
    var performanceMode: Boolean by mutableStateOf(false)
        private set

    private var loaded = false

    /** Reads the persisted value once, at startup. */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        performanceMode = prefs(context).getBoolean(KEY_PERFORMANCE, false)
    }

    fun setPerformanceMode(context: Context, enabled: Boolean) {
        performanceMode = enabled
        prefs(context).edit().putBoolean(KEY_PERFORMANCE, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * Whether glass surfaces should sample a backdrop at all.
 *
 * False when the user turned on performance mode, and false on devices below
 * Android 12, where there is no GPU blur: sampling live content there would
 * show it *sharp* inside the glass, which looks broken rather than clear.
 * Surfaces fall back to an opaque fill in both cases.
 */
val isLiquidGlassEnabled: Boolean
    get() = isGlassBlurSupported && !GlassSettings.performanceMode
