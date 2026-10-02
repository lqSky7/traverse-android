package com.traverse.android.ui.theme

import androidx.compose.ui.graphics.Color

// Monochrome palette
val Black = Color(0xFF000000)
val DarkGray = Color(0xFF1A1A1A)
val MediumGray = Color(0xFF404040)
val Gray = Color(0xFF808080)
val LightGray = Color(0xFFD0D0D0)
val OffWhite = Color(0xFFF5F5F5)
val White = Color(0xFFFFFFFF)
val Peach = Color(0xFFFFB08A)

/**
 * The surface every card in the app sits on.
 *
 * By value this is [DarkGray], but named for what it means rather than what it looks like. Thirty-odd
 * files each declared their own `private val CardBackground = Color(0xFF1A1A1A)` instead of reaching
 * for this, and that is exactly how one of them ended up on `#1C1C1C` without anyone noticing — a
 * copy of a hex is a place for the hex to drift.
 *
 * A sheet uses this same colour as its container, so a panel *inside* a sheet has to lift off it
 * rather than repeat it, or it disappears — see [SheetPanelBackground].
 */
val CardBackground = DarkGray

/**
 * A panel *inside* a sheet.
 *
 * A sheet's container is already [CardBackground], so a panel drawn on one has to lift off it rather
 * than repeat it — repeat the colour and the panel disappears. A translucent white over the sheet is
 * the treatment `AllAtRiskProblemsSheet` established, and this is that value given a name.
 *
 * Not to be confused with [CardBackground]: a card sits on the black screen, a sheet panel sits on
 * the sheet.
 */
val SheetPanelBackground = Color.White.copy(alpha = 0.05f)
