package com.fountain.launcher.fountain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fountain.launcher.ui.theme.FountainPalette
import kotlin.random.Random

/**
 * The animated pixel fountain (spec §2.6). Purple particles arc up from a base and
 * cascade back down, drawn as hard-edged cells snapped to a grid (nearest-neighbor feel).
 *
 * Battery-conscious: the simulation ticks at a capped rate and only runs while [running]
 * is true — the lock overlay passes `false` when it's not visible.
 */
@Composable
fun PixelFountain(
    modifier: Modifier = Modifier,
    running: Boolean = true,
    cell: Dp = 3.dp,
    fps: Int = 20,
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }

    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val particles = remember { ArrayList<Particle>() }
        var frame by remember { mutableLongStateOf(0L) }

        LaunchedFountainLoop(running, fps) {
            step(particles, widthPx, heightPx)
            frame = frame + 1
        }

        Canvas(Modifier.matchParentSize()) {
            @Suppress("UNUSED_EXPRESSION") frame // subscribe: redraw each simulated step
            for (p in particles) {
                val gx = (p.x / cellPx).toInt() * cellPx
                val gy = (p.y / cellPx).toInt() * cellPx
                drawRect(color = p.color(), topLeft = Offset(gx, gy), size = Size(cellPx, cellPx))
            }
        }
    }
}

@Composable
private fun LaunchedFountainLoop(running: Boolean, fps: Int, onStep: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(running, fps) {
        if (!running) return@LaunchedEffect
        val stepNs = 1_000_000_000L / fps
        var last = 0L
        var acc = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                acc += now - last
                last = now
                if (acc >= stepNs) {
                    acc = 0
                    onStep()
                }
            }
        }
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Int,
    val originY: Float,
    val span: Float,
) {
    fun color(): Color {
        // Rises from deep violet at the base to magenta highlight near the top.
        val t = ((originY - y) / span).coerceIn(0f, 1f)
        return if (t < 0.5f) {
            lerp(FountainPalette.PurpleDeep, FountainPalette.PurplePrimary, t * 2f)
        } else {
            lerp(FountainPalette.PurplePrimary, FountainPalette.MagentaHi, (t - 0.5f) * 2f)
        }
    }
}

private const val MAX_PARTICLES = 520

private fun step(particles: ArrayList<Particle>, width: Float, height: Float) {
    if (width <= 0f || height <= 0f) return
    val baseY = height * 0.98f
    val baseX = width * 0.5f
    val span = height * 0.94f

    // A Deltarune-style geyser: a tight column launches straight up from the base and
    // widens as it rises, dissolving at the very top. No gravity — nothing ever falls.
    if (particles.size < MAX_PARTICLES) {
        repeat(7) {
            val core = it < 3 // a brighter, tighter central jet
            particles.add(
                Particle(
                    x = baseX + (Random.nextFloat() - 0.5f) * width * (if (core) 0.03f else 0.09f),
                    y = baseY,
                    vx = (Random.nextFloat() - 0.5f) * 1.4f,
                    vy = -(7f + Random.nextFloat() * 5f),
                    life = 220,
                    originY = baseY,
                    span = span,
                )
            )
        }
    }

    val topLimit = height * 0.04f
    val iterator = particles.iterator()
    while (iterator.hasNext()) {
        val p = iterator.next()
        p.x += p.vx
        p.y += p.vy
        p.vx *= 1.01f          // fan outward as it rises, like a fountain crown
        p.life--
        if (p.life <= 0 || p.y < topLimit || p.x < 0f || p.x > width) iterator.remove()
    }
}
