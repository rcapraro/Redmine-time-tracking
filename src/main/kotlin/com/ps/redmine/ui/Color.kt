package com.ps.redmine.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Modern Material 3 color schemes.
 *
 * **Palette** — Violet as the primary brand accent, Emerald for success/secondary, Pink for
 * the tertiary accent, and Red for error. The four roles are intentionally distinct hues so
 * UI elements that map onto different roles (selected items / status rows / today's date /
 * errors) are immediately distinguishable. The 600-tier saturation level lands modern and
 * vibrant without crossing into neon.
 *
 * **Surfaces (Light)** — A subtle cool-tinted off-white body lets pure-white panels pop as
 * clean cards. The `surfaceContainer` (entry rows) uses a faint cool gray to zebra against
 * white panels, and `surfaceContainerHigh` (day headers, pills, dialog containers) carries a
 * pale Violet wash so brand color shows up at the section-banner level. Inset surfaces nest
 * with the deeper `primaryContainer`-family Violet. Net effect: bright, calm, branded.
 *
 * **Surfaces (Dark)** — A deep cool body in the same Violet family, with the elevation
 * ladder rising through cool charcoals; `surfaceContainerHigh` keeps the Violet cast so the
 * day banner reads as a tinted band on the dark panels.
 */

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDD6FE),
    onPrimaryContainer = Color(0xFF2E1065),
    inversePrimary = Color(0xFFC4B5FD),

    secondary = Color(0xFF059669),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA7F3D0),
    onSecondaryContainer = Color(0xFF052E16),

    tertiary = Color(0xFFDB2777),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBCFE8),
    onTertiaryContainer = Color(0xFF500724),

    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFECACA),
    onErrorContainer = Color(0xFF7F1D1D),

    // Cool-tinted off-white body — the slight Violet undertone keeps the canvas reading
    // as warm-modern rather than cold gray.
    background = Color(0xFFF8F7FB),
    onBackground = Color(0xFF1B1923),

    surface = Color(0xFFF8F7FB),
    onSurface = Color(0xFF1B1923),
    surfaceVariant = Color(0xFFE5E2EC),
    onSurfaceVariant = Color(0xFF49474F),
    surfaceTint = Color(0xFF7C3AED),

    inverseSurface = Color(0xFF2F2C36),
    inverseOnSurface = Color(0xFFF5F4FA),

    outline = Color(0xFF8C8995),
    outlineVariant = Color(0xFFCBC9D3),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFFDAD7E0),
    surfaceBright = Color(0xFFFFFFFF),
    // Inverted, branded surface ladder:
    //   body cool off-white  →  panels white  →  entry rows pale gray
    //   →  day-header / pills / dialog containers pale Violet
    //   →  inset boxes deeper Violet
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F0F6),
    surfaceContainerHigh = Color(0xFFF5F3FF),
    surfaceContainerHighest = Color(0xFFEDE9FE),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE9FE),
    inversePrimary = Color(0xFF7C3AED),

    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),

    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF9D174D),
    onTertiaryContainer = Color(0xFFFCE7F3),

    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),

    background = Color(0xFF16151E),
    onBackground = Color(0xFFE5E4EC),

    surface = Color(0xFF16151E),
    onSurface = Color(0xFFE5E4EC),
    surfaceVariant = Color(0xFF2C2A36),
    onSurfaceVariant = Color(0xFFC8C6D0),
    surfaceTint = Color(0xFFC4B5FD),

    inverseSurface = Color(0xFFE5E4EC),
    inverseOnSurface = Color(0xFF16151E),

    outline = Color(0xFF6F6D7B),
    outlineVariant = Color(0xFF43414F),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFF16151E),
    surfaceBright = Color(0xFF363441),
    // Mirror of the light ladder (body deepest, day-header tinted Violet):
    surfaceContainerLowest = Color(0xFF111016),
    surfaceContainerLow = Color(0xFF201E29),
    surfaceContainer = Color(0xFF26242F),
    surfaceContainerHigh = Color(0xFF2C283C),
    surfaceContainerHighest = Color(0xFF36304C),
)

/** Amber — used for warning snackbars (M3 has no semantic warning role). */
val WarningAccentLight = Color(0xFFD97706)
val WarningAccentDark = Color(0xFFFCD34D)

/** Foreground paired with the warning accent above (legible on each amber). */
val WarningOnAccentLight = Color(0xFFFFFFFF)
val WarningOnAccentDark = Color(0xFF422006)

/**
 * Diverse confetti palette — violets, purples, pink, peach. Deliberately omits red/yellow/
 * green, which carry error/warning/in-progress semantics elsewhere in the app and clash
 * with a celebratory feel.
 */
val ConfettiPaletteLight = listOf(
    Color(0xFF7C3AED), // Violet-600
    Color(0xFF8B5CF6), // Violet-500
    Color(0xFFA78BFA), // Violet-400
    Color(0xFF6366F1), // Indigo-500
    Color(0xFFA855F7), // Purple-500
    Color(0xFFDB2777), // Pink
    Color(0xFFF97316), // Orange
    Color(0xFFFB923C), // Orange-light
)
val ConfettiPaletteDark = listOf(
    Color(0xFFC4B5FD), // Violet-300
    Color(0xFFA78BFA), // Violet-400
    Color(0xFFDDD6FE), // Violet-200
    Color(0xFFA5B4FC), // Indigo-300
    Color(0xFFD8B4FE), // Purple-300
    Color(0xFFF472B6), // Pink-light
    Color(0xFFFB923C), // Orange-light
    Color(0xFFFDBA74), // Orange-pale
)

/**
 * Scrollbar thumb colors — neutral grays with explicit alpha so the bar is actually
 * visible on both surfaces (Compose Desktop's default style is black-on-anything at 12%
 * alpha, which disappears on dark backgrounds).
 */
val ScrollbarThumbLight = Color(0xFF6F6D7B).copy(alpha = 0.40f)
val ScrollbarThumbHoverLight = Color(0xFF6F6D7B).copy(alpha = 0.70f)
val ScrollbarThumbDark = Color(0xFFA6A4AC).copy(alpha = 0.40f)
val ScrollbarThumbHoverDark = Color(0xFFA6A4AC).copy(alpha = 0.75f)
