package com.traverse.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.traverse.android.R

// Custom fonts
val RingiftFamily = FontFamily(Font(R.font.ringift))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono_regular))
val BelfastGroteskBlackFamily = FontFamily(Font(R.font.belfast_grotesk_black, FontWeight.Black))
val HarmonyOsSansFamily = FontFamily(
    Font(R.font.harmonyos_sans_regular, FontWeight.Normal),
    Font(R.font.harmonyos_sans_bold, FontWeight.Bold)
)

val Typography = Typography(
    // Display - Belfast Grotesk Black for big headers
    displayLarge = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = 2.sp
    ),
    displayMedium = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 1.5.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 1.sp
    ),
    // Headlines - Belfast Grotesk Black
    headlineLarge = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.25.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BelfastGroteskBlackFamily,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.25.sp
    ),
    // Title - HarmonyOS Sans for buttons
    titleLarge = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body - HarmonyOS Sans for form labels/inputs
    bodyLarge = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Labels - HarmonyOS Sans small
    labelLarge = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HarmonyOsSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
