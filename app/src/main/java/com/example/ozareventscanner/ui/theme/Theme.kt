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
    primary          = ColorPrimaryVibrant,
    onPrimary        = ColorForeground,
    secondary        = ColorAccent,
    onSecondary      = ColorAccentForeground,
    background       = ColorDark,
    onBackground     = ColorBackground,
    surface          = GrayDark3,
    onSurface        = ColorBackground,
    outline          = GrayDark1,
    surfaceVariant   = GrayDark4
)

private val LightColorScheme = lightColorScheme(
    primary          = ColorPrimary,
    onPrimary        = ColorPrimaryForeground,
    secondary        = ColorSecondary,
    onSecondary      = ColorSecondaryForeground,
    background       = ColorBackground,
    onBackground     = ColorTextPrimary,
    surface          = ColorSurface,
    onSurface        = ColorTextPrimary,
    outline          = ColorBorder,
    surfaceVariant   = ColorSurfaceHigh
)

@Composable
fun OzarEventScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes      = AppShapes,
        typography  = Typography,
        content     = content
    )
}