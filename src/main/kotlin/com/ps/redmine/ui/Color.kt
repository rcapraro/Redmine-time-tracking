package com.ps.redmine.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Catppuccin-flavored Material 3 color schemes — softer, more pastel than the previous
 * GitHub Primer palette. Two flavors are exposed:
 * - Light = **Latte**   (https://github.com/catppuccin/catppuccin#-palette)
 * - Dark  = **Macchiato**
 *
 * Mapping convention (uniform across both flavors):
 * - primary   → blue   (brand accent)
 * - secondary → green  (used for progress fills)
 * - tertiary  → mauve
 * - error     → red
 *
 * For dark surfaces the M3 elevation ladder maps onto Catppuccin's surface tokens
 * (crust < mantle < base < surface0 < surface1 < surface2). For light, base is the
 * brightest tone and we descend through mantle / crust / surface0 / surface1.
 */

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E66F5),
    onPrimary = Color(0xFFEFF1F5),
    primaryContainer = Color(0xFFCFE0FF),
    onPrimaryContainer = Color(0xFF002B69),
    inversePrimary = Color(0xFF7287FD),

    secondary = Color(0xFF40A02B),
    onSecondary = Color(0xFFEFF1F5),
    secondaryContainer = Color(0xFFCFEACB),
    onSecondaryContainer = Color(0xFF0D3F00),

    tertiary = Color(0xFF8839EF),
    onTertiary = Color(0xFFEFF1F5),
    tertiaryContainer = Color(0xFFE6D6FF),
    onTertiaryContainer = Color(0xFF2C0F62),

    error = Color(0xFFD20F39),
    onError = Color(0xFFEFF1F5),
    errorContainer = Color(0xFFFFD9DD),
    onErrorContainer = Color(0xFF410014),

    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),

    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF5C5F77),
    surfaceTint = Color(0xFF1E66F5),

    inverseSurface = Color(0xFF4C4F69),
    inverseOnSurface = Color(0xFFEFF1F5),

    outline = Color(0xFF8C8FA1),
    outlineVariant = Color(0xFFBCC0CC),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFFCCD0DA),
    surfaceBright = Color(0xFFEFF1F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFE6E9EF),
    surfaceContainer = Color(0xFFDCE0E8),
    surfaceContainerHigh = Color(0xFFCCD0DA),
    surfaceContainerHighest = Color(0xFFBCC0CC),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AADF4),
    onPrimary = Color(0xFF24273A),
    primaryContainer = Color(0xFF34528C),
    onPrimaryContainer = Color(0xFFD3DEFA),
    inversePrimary = Color(0xFFB7BDF8),

    secondary = Color(0xFFA6DA95),
    onSecondary = Color(0xFF24273A),
    secondaryContainer = Color(0xFF345733),
    onSecondaryContainer = Color(0xFFDCEFD2),

    tertiary = Color(0xFFC6A0F6),
    onTertiary = Color(0xFF24273A),
    tertiaryContainer = Color(0xFF513977),
    onTertiaryContainer = Color(0xFFEED9FB),

    error = Color(0xFFED8796),
    onError = Color(0xFF24273A),
    errorContainer = Color(0xFF6F2935),
    onErrorContainer = Color(0xFFFFD9DF),

    background = Color(0xFF24273A),
    onBackground = Color(0xFFCAD3F5),

    surface = Color(0xFF24273A),
    onSurface = Color(0xFFCAD3F5),
    surfaceVariant = Color(0xFF363A4F),
    onSurfaceVariant = Color(0xFFB8C0E0),
    surfaceTint = Color(0xFF8AADF4),

    inverseSurface = Color(0xFFCAD3F5),
    inverseOnSurface = Color(0xFF24273A),

    outline = Color(0xFF8087A2),
    outlineVariant = Color(0xFF494D64),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFF1E2030),
    surfaceBright = Color(0xFF5B6078),
    surfaceContainerLowest = Color(0xFF181926),
    surfaceContainerLow = Color(0xFF1E2030),
    surfaceContainer = Color(0xFF363A4F),
    surfaceContainerHigh = Color(0xFF494D64),
    surfaceContainerHighest = Color(0xFF5B6078),
)

/** Catppuccin yellow — used for warning snackbars (M3 has no semantic warning role). */
val WarningAccentLight = Color(0xFFDF8E1D)
val WarningAccentDark = Color(0xFFEED49F)

/** Foreground paired with the warning accent above (legible on each yellow). */
val WarningOnAccentLight = Color(0xFFFFFFFF)
val WarningOnAccentDark = Color(0xFF24273A)

/**
 * "Cool celebration" Catppuccin accent palette for the confetti overlay — purples, blues,
 * pink, and a peach/rosewater warm note. Deliberately omits red/yellow/green, which carry
 * error/warning/in-progress semantics elsewhere in the app and clash with a celebratory feel.
 *
 * Latte values from https://catppuccin.com/palette ; Macchiato likewise.
 */
val ConfettiPaletteLight = listOf(
    Color(0xFF1E66F5), // Blue
    Color(0xFF209FB5), // Sapphire
    Color(0xFF04A5E5), // Sky
    Color(0xFF7287FD), // Lavender
    Color(0xFF8839EF), // Mauve
    Color(0xFFEA76CB), // Pink
    Color(0xFFFE640B), // Peach
    Color(0xFFDC8A78), // Rosewater
)
val ConfettiPaletteDark = listOf(
    Color(0xFF8AADF4), // Blue
    Color(0xFF7DC4E4), // Sapphire
    Color(0xFF91D7E3), // Sky
    Color(0xFFB7BDF8), // Lavender
    Color(0xFFC6A0F6), // Mauve
    Color(0xFFF5BDE6), // Pink
    Color(0xFFF5A97F), // Peach
    Color(0xFFF4DBD6), // Rosewater
)

/**
 * Scrollbar thumb colors — Catppuccin overlay tones with explicit alpha so the bar is
 * actually visible on both surfaces (Compose Desktop's default style is black-on-anything
 * at 12% alpha, which disappears on dark backgrounds).
 */
val ScrollbarThumbLight = Color(0xFF6C6F85).copy(alpha = 0.45f)         // Latte subtext0
val ScrollbarThumbHoverLight = Color(0xFF6C6F85).copy(alpha = 0.75f)
val ScrollbarThumbDark = Color(0xFFA5ADCB).copy(alpha = 0.45f)          // Macchiato subtext0
val ScrollbarThumbHoverDark = Color(0xFFA5ADCB).copy(alpha = 0.80f)
