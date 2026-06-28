package com.dmgproductions.amp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A radial spectrum visualizer that rings the album art. Each bar is one band of
 * [levels] (0..1); a slow counter-rotating glow ring adds life. Pure Compose
 * Canvas — the modern replacement for the old View-based VisualizerView.
 */
@Composable
fun CircularVisualizer(
    levels: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spinAngle",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = min(size.width, size.height) / 2f
        val innerR = maxR * 0.74f
        val maxBar = maxR * 0.24f
        val barCount = levels.size.coerceAtLeast(1)

        // Soft glow halo behind the artwork.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = innerR * 1.15f,
            ),
            radius = innerR * 1.15f,
            center = center,
        )

        // Counter-rotating faint ring.
        rotate(degrees = -spin * 0.4f, pivot = center) {
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = innerR * 1.02f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = maxR * 0.012f),
            )
        }

        // Radial spectrum bars.
        for (i in 0 until barCount) {
            val level = levels[i].coerceIn(0f, 1f)
            val angle = (i.toFloat() / barCount) * (2.0 * Math.PI).toFloat() - (Math.PI / 2f).toFloat()
            val barLen = maxBar * (0.18f + level * 0.82f)
            val cosA = cos(angle)
            val sinA = sin(angle)
            val start = Offset(center.x + cosA * innerR, center.y + sinA * innerR)
            val end = Offset(center.x + cosA * (innerR + barLen), center.y + sinA * (innerR + barLen))
            drawLine(
                color = accent.copy(alpha = 0.35f + level * 0.65f),
                start = start,
                end = end,
                strokeWidth = maxR * 0.022f,
                cap = StrokeCap.Round,
            )
        }
    }
}
