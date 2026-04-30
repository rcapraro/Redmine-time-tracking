package com.ps.redmine.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * Returns a horizontal gradient brush that animates left→right, suitable for
 * `Modifier.background(brush)` on a placeholder Surface.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainer

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 600f, 0f),
        end = Offset(translate, 0f),
    )
}

/**
 * Skeleton placeholder for the time-entries list while loading.
 */
@Composable
fun TimeEntriesListSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(3) { groupIndex ->
            // Date header bar
            SkeletonBar(brush = shimmer, height = 36.dp)
            // A couple of entry rows beneath
            val rows = if (groupIndex == 0) 2 else 1
            repeat(rows) {
                SkeletonBar(brush = shimmer, height = 56.dp)
            }
        }
    }
}

@Composable
private fun SkeletonBar(brush: Brush, height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush),
    )
}
