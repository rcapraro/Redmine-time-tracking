package com.ps.redmine.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val color: Color,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val width: Float,
    val height: Float,
)

/**
 * Subtle party-popper confetti emitted from the two upper corners, with an optional
 * centered banner that fades in/out on the same timeline. Re-fires every time
 * [triggerKey] changes to a non-zero value. Drawn full-size — place inside a Box so
 * it overlays the rest of the UI. Does not consume pointer events.
 */
@Composable
fun ConfettiOverlay(
    triggerKey: Int,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    durationMs: Int = 3200,
    particleCount: Int = 110,
) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var fade by remember { mutableStateOf(1f) }
    var bannerAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        while (canvasSize == Size.Zero) withFrameNanos { }

        val w = canvasSize.width
        val h = canvasSize.height
        val rng = Random(triggerKey.toLong() xor System.nanoTime())
        val emitters = listOf(
            Offset(w * 0.14f, h * 0.18f),
            Offset(w * 0.86f, h * 0.18f),
        )
        particles = List(particleCount) { i ->
            val origin = emitters[i % emitters.size]
            val fromLeft = origin.x < w / 2f
            // Aim toward the upper area in a cone (party-popper feel)
            val baseAngle = if (fromLeft) -Math.PI.toFloat() / 4f else -Math.PI.toFloat() * 3f / 4f
            val spread = (Math.PI.toFloat() / 2.0f)
            val angle = baseAngle + (rng.nextFloat() - 0.5f) * spread
            val speed = 480f + rng.nextFloat() * 360f
            ConfettiParticle(
                color = palette[rng.nextInt(palette.size)],
                x = origin.x,
                y = origin.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                rotation = rng.nextFloat() * 360f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 360f,
                width = 12f + rng.nextFloat() * 10f,
                height = 18f + rng.nextFloat() * 12f,
            )
        }
        fade = 1f
        bannerAlpha = 0f

        val gravity = 800f
        val drag = 0.987f
        val start = withFrameNanos { it }
        var last = start
        while (true) {
            val now = withFrameNanos { it }
            val elapsedMs = (now - start) / 1_000_000.0
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
            last = now

            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + p.vy * dt,
                    vx = p.vx * drag,
                    vy = p.vy * drag + gravity * dt,
                    rotation = p.rotation + p.rotationSpeed * dt,
                )
            }
            val progress = (elapsedMs / durationMs).toFloat().coerceIn(0f, 1f)
            // Hold full opacity for the first 65% of the animation, then fade out.
            fade = if (progress < 0.65f) 1f else 1f - (progress - 0.65f) / 0.35f
            // Banner fades in over the first 12%, holds, then fades out over the last 25%.
            bannerAlpha = when {
                progress < 0.12f -> progress / 0.12f
                progress < 0.75f -> 1f
                else -> 1f - (progress - 0.75f) / 0.25f
            }.coerceIn(0f, 1f)
            if (elapsedMs >= durationMs) break
        }
        particles = emptyList()
        fade = 0f
        bannerAlpha = 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
        ) {
            if (particles.isEmpty()) return@Canvas
            particles.forEach { p ->
                rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                    drawRect(
                        color = p.color.copy(alpha = fade.coerceIn(0f, 1f)),
                        topLeft = Offset(p.x - p.width / 2f, p.y - p.height / 2f),
                        size = Size(p.width, p.height),
                    )
                }
            }
        }

        if (bannerAlpha > 0f && (title != null || subtitle != null)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(bannerAlpha),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Celebration,
                        contentDescription = null,
                    )
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
