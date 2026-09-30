package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TivionsDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF04101A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = AccentCyanLight,
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF282846),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = AccentGold,
    background = BgDarkNavy,
    onBackground = TextPrimary,
    surface = BgCardDark,
    onSurface = TextPrimary,
    surfaceVariant = BgCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Tivions has a signature deep OLED dark aesthetic
    MaterialTheme(
        colorScheme = TivionsDarkColorScheme,
        typography = Typography,
        content = content
    )
}
