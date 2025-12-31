package com.example.traverse2.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom colors for glassmorphism
data class GlassColors(
    val glass: Color,
    val glassBorder: Color,
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    val success: Color,
    val error: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    // Orb colors for animated background
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val orb4: Color,
    val orb5: Color
)

// Theme state object for toggling dark mode
object ThemeState {
    var isDarkMode by mutableStateOf(false)
    
    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
}

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        glass = GlassLight,
        glassBorder = GlassBorderLight,
        gradientStart = GradientLightStart,
        gradientMiddle = GradientLightMiddle,
        gradientEnd = GradientLightEnd,
        success = LightSuccess,
        error = LightError,
        accent = LightPrimary,
        accentSecondary = LightSecondary,
        textPrimary = LightOnBackground,
        textSecondary = LightOnSurface,
        isDark = false,
        orb1 = LightOrb1,
        orb2 = LightOrb2,
        orb3 = LightOrb3,
        orb4 = LightOrb4,
        orb5 = LightOrb5
    )
}

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    error = LightError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    error = DarkError
)

private val LightGlassColors = GlassColors(
    glass = GlassLight,
    glassBorder = GlassBorderLight,
    gradientStart = GradientLightStart,
    gradientMiddle = GradientLightMiddle,
    gradientEnd = GradientLightEnd,
    success = LightSuccess,
    error = LightError,
    accent = LightPrimary,
    accentSecondary = LightSecondary,
    textPrimary = LightOnBackground,
    textSecondary = LightOnSurface,
    isDark = false,
    orb1 = LightOrb1,
    orb2 = LightOrb2,
    orb3 = LightOrb3,
    orb4 = LightOrb4,
    orb5 = LightOrb5
)

private val DarkGlassColors = GlassColors(
    glass = GlassDark,
    glassBorder = GlassBorderDark,
    gradientStart = GradientDarkStart,
    gradientMiddle = GradientDarkMiddle,
    gradientEnd = GradientDarkEnd,
    success = DarkSuccess,
    error = DarkError,
    accent = DarkPrimary,
    accentSecondary = DarkSecondary,
    textPrimary = DarkOnBackground,
    textSecondary = DarkOnSurface,
    isDark = true,
    orb1 = DarkOrb1,
    orb2 = DarkOrb2,
    orb3 = DarkOrb3,
    orb4 = DarkOrb4,
    orb5 = DarkOrb5
)

@Composable
fun TraverseTheme(
    darkTheme: Boolean = ThemeState.isDarkMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            // Status bar icons: light icons on dark background, dark icons on light background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension to access glass colors
object TraverseTheme {
    val glassColors: GlassColors
        @Composable
        get() = LocalGlassColors.current
}