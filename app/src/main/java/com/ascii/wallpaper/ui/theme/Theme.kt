package com.ascii.wallpaper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalColorScheme = darkColorScheme(
    primary = TerminalAccent,
    onPrimary = TerminalBlack,
    primaryContainer = TerminalCard,
    onPrimaryContainer = TerminalAccent,
    secondary = TerminalAccent,
    onSecondary = TerminalBlack,
    tertiary = TerminalAmber,
    background = TerminalBackground,
    onBackground = TerminalText,
    surface = TerminalCard,
    onSurface = TerminalText,
    surfaceVariant = TerminalCard,
    onSurfaceVariant = TerminalTextDim,
    outline = TerminalBorder,
)

@Composable
fun AsciiWallpapersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        content = content,
    )
}
