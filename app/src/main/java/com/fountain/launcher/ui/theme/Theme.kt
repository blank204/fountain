package com.fountain.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Fountain is a dark, purple-forward theme. The launcher home surface renders
 * monochrome (applied at the icon/render layer, spec §2.2); the base scheme here
 * carries the Dark Fountain purples used by the lock overlay and notification views.
 */
private val FountainColors = darkColorScheme(
    primary = FountainPalette.PurplePrimary,
    onPrimary = FountainPalette.Mono6,
    secondary = FountainPalette.MagentaHi,
    tertiary = FountainPalette.CyanGlow,
    background = FountainPalette.Background,
    onBackground = FountainPalette.Mono5,
    surface = FountainPalette.Surface,
    onSurface = FountainPalette.Mono5,
)

@Composable
fun FountainTheme(
    // Fountain is always dark; the param exists so previews can override.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FountainColors,
        typography = FountainTypography,
        content = content,
    )
}
