package com.ps.redmine.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 color schemes generated from a vivid royal-blue seed (#2956D9) with a
 * violet tertiary accent — same structure as the Material Theme Builder output, but
 * tuned for more saturation and energy than the baseline blue.
 */

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2956D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF001457),
    inversePrimary = Color(0xFFB6C4FF),

    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171A2C),

    tertiary = Color(0xFF7B4FFF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECDDFF),
    onTertiaryContainer = Color(0xFF260054),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFBFBFF),
    onBackground = Color(0xFF1B1B21),

    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    surfaceTint = Color(0xFF2956D9),

    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF2F0F4),

    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC7C5D0),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFFDBD9DD),
    surfaceBright = Color(0xFFFBFBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3F7),
    surfaceContainer = Color(0xFFEFEDF1),
    surfaceContainerHigh = Color(0xFFE9E7EB),
    surfaceContainerHighest = Color(0xFFE3E1E5),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF00298E),
    primaryContainer = Color(0xFF023ABE),
    onPrimaryContainer = Color(0xFFDCE3FF),
    inversePrimary = Color(0xFF2956D9),

    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),

    tertiary = Color(0xFFD2BCFF),
    onTertiary = Color(0xFF3F0E84),
    tertiaryContainer = Color(0xFF582DC2),
    onTertiaryContainer = Color(0xFFECDDFF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E6),

    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceTint = Color(0xFFB6C4FF),

    inverseSurface = Color(0xFFE4E1E6),
    inverseOnSurface = Color(0xFF303034),

    outline = Color(0xFF91909A),
    outlineVariant = Color(0xFF45464F),
    scrim = Color(0xFF000000),

    surfaceDim = Color(0xFF131318),
    surfaceBright = Color(0xFF39383D),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B20),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A292F),
    surfaceContainerHighest = Color(0xFF35343A),
)

/**
 * Amber accent used for warning snackbars (M3 has no semantic warning role).
 */
val WarningAccentLight = Color(0xFFB26500)
val WarningAccentDark = Color(0xFFFFB95C)
