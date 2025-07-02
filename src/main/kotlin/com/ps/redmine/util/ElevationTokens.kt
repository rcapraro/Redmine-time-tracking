package com.ps.redmine.util

import androidx.compose.ui.unit.dp

/**
 * Consistent elevation tokens for the application.
 * These provide a semantic approach to elevation that ensures visual consistency.
 */
object ElevationTokens {
    /**
     * No elevation - for elements that should appear flat on the surface
     */
    val None = 0.dp

    /**
     * Low elevation - for subtle separation from background
     * Used for: secondary content areas, technical details, dropdown content
     */
    val Low = 1.dp

    /**
     * Medium elevation - for primary content areas and containers
     * Used for: date headers, cards, main content surfaces
     */
    val Medium = 2.dp

    /**
     * High elevation - for interactive elements and selected states
     * Used for: selected items, focused elements, important interactive surfaces
     */
    val High = 3.dp
}