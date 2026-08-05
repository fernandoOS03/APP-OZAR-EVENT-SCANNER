package com.example.ozareventscanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    secondary = ColorAccent,
    background = ColorDark,
    surface = ColorSurface,
    onPrimary = ColorSurface,
    onBackground = ColorTextPrimary,
    onSurface = ColorTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    secondary = ColorAccent,
    background = ColorDark, // ColorDark is Color(0xFFF9FAFB)
    surface = ColorSurface, // Color(0xFFFFFFFF)
    onPrimary = ColorSurface,
    onBackground = ColorTextPrimary,
    onSurface = ColorTextPrimary
)

@Composable
fun OzarEventScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to keep brand guidelines (Red/White theme)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We prioritize our custom Claro/Ozar light theme for this brand adaptation
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // In light theme, we want dark icons in the status bar (isAppearanceLightStatusBars = true)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}