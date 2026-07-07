package com.fountain.launcher.fountain

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * CRT scanline + vignette overlay with a very subtle flicker (spec §2.6, toggleable).
 * Draw it above the fountain. When [enabled] is false it renders nothing.
 */
@Composable
fun CrtOverlay(modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (!enabled) return

    val transition = rememberInfiniteTransition(label = "crt")
    val flicker by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(140), RepeatMode.Reverse),
        label = "flicker",
    )

    Canvas(modifier) {
        val scanColor = Color.Black.copy(alpha = 0.16f * flicker)
        var y = 0f
        while (y < size.height) {
            drawRect(color = scanColor, topLeft = Offset(0f, y), size = Size(size.width, 1f))
            y += 3f
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.75f,
            ),
        )
    }
}
