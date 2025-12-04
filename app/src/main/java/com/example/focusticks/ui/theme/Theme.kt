package com.example.focusticks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightText,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigoDark,
    onPrimary = Color.White,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkText,
    error = ErrorRed
)

@Composable
fun FocuSticksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(),
        content = content
    )
}
