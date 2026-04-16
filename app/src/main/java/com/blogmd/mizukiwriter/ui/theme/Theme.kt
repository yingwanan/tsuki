package com.blogmd.mizukiwriter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MizukiPinkDark,
    onPrimary = Color.White,
    secondary = MizukiBlue,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = CardSurfaceLight,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEAE2E8),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFFE0E0E0)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = BackgroundDark,
    secondary = DarkSecondary,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextLight,
    surface = CardSurfaceDark,
    onSurface = TextLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = DarkBorder,
)

@Composable
fun MizukiWriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
