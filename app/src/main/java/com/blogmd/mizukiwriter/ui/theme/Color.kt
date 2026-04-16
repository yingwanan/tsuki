package com.blogmd.mizukiwriter.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MizukiPink = Color(0xFFFFB6C1)
val MizukiPinkDark = Color(0xFFF06292)
val MizukiBlue = Color(0xFF87CEFA)

val BackgroundLight = Color(0xFFFDFDFD)
val BackgroundDark = Color(0xFF0B0D10)

val CardSurfaceLight = Color(0xFFF2EAF1)
val CardSurfaceDark = Color(0xFF171A1F)
val SwipeCardMaskLight = Color(0xFFFFFFFF)
val SwipeCardMaskDark = Color(0xFF20242B)

val TextDark = Color(0xFF2C2C2C)
val TextLight = Color(0xFFF5F7FA)
val TextSecondaryDark = Color(0xFF6B6B6B)
val TextSecondaryLight = Color(0xFFB4BDC9)

val GlassWhite = Color(0xFFF7F7F7)
val GlassDark = Color(0xFF111419)

val DarkPrimary = Color(0xFFB8C4D4)
val DarkSecondary = Color(0xFFD7DEE7)
val DarkBorder = Color(0xFF333A45)
val DarkSurfaceVariant = Color(0xFF20242B)
val DarkSurfaceElevated = Color(0xFF15181D)
val LightBackgroundScrim = Color(0xBFF7F7F7)
val DarkBackgroundScrim = Color(0xCC0B0D10)
val appTopBarVerticalPadding: Dp = 0.dp
val appTopBarMinHeight: Dp = 32.dp

fun appChromeContainerColor(isDarkTheme: Boolean): Color = if (isDarkTheme) GlassDark else GlassWhite

fun appBackgroundImageScrim(isDarkTheme: Boolean): Color = if (isDarkTheme) DarkBackgroundScrim else LightBackgroundScrim

fun appTopBarContainerColor(isDarkTheme: Boolean): Color = if (isDarkTheme) BackgroundDark else BackgroundLight
