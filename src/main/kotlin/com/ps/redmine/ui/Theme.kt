package com.ps.redmine.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Active dark/light state — exposed so non-MaterialTheme components (e.g. snackbars
 * styled outside the M3 ColorScheme) react to the chosen flavor instead of the OS.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun RedmineTimeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val scrollbarStyle = ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 10.dp,
        shape = RoundedCornerShape(5.dp),
        hoverDurationMillis = 200,
        unhoverColor = if (darkTheme) ScrollbarThumbDark else ScrollbarThumbLight,
        hoverColor = if (darkTheme) ScrollbarThumbHoverDark else ScrollbarThumbHoverLight,
    )

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalScrollbarStyle provides scrollbarStyle,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}
