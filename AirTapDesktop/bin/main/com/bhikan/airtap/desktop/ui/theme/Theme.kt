package com.bhikan.airtap.desktop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4ecca3),
    onPrimary = Color(0xFF1a1a2e),
    primaryContainer = Color(0xFF16213e),
    secondary = Color(0xFF4ecca3),
    background = Color(0xFF1a1a2e),
    surface = Color(0xFF16213e),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFe74c3c)
)

@Composable
fun AirTapDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
