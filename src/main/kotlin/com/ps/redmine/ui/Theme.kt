package com.ps.redmine.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun RedmineTimeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = appTypography(),
        shapes = AppShapes,
        content = content,
    )
}
