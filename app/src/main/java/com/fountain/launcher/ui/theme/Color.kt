package com.fountain.launcher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark Fountain palette (spec §3). These are the single source of truth for color;
 * screens should reference the theme, not hardcode hex values.
 */
object FountainPalette {
    // Core surface
    val Background = Color(0xFF0B0B10)   // near-black
    val Surface = Color(0xFF14141C)

    // Purple fountain ramp
    val PurplePrimary = Color(0xFF7A3CFF)
    val PurpleDeep = Color(0xFF3A1E6E)
    val MagentaHi = Color(0xFFC74BFF)

    // Accents (use sparingly)
    val CyanGlow = Color(0xFF4DE0E0)

    // Monochrome ramp for the launcher home surface
    val Mono0 = Color(0xFF000000)
    val Mono1 = Color(0xFF1A1A1A)
    val Mono2 = Color(0xFF3A3A3A)
    val Mono3 = Color(0xFF6E6E6E)
    val Mono4 = Color(0xFFAEAEAE)
    val Mono5 = Color(0xFFE6E6E6)
    val Mono6 = Color(0xFFFFFFFF)
}
