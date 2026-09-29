package com.intellinav.ui.theme

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
    primary = ElectricCyan,
    secondary = MintGreen,
    tertiary = AmberWarning,
    background = DeepSpaceNavy,
    surface = SurfaceSlate,
    surfaceContainer = CardSurface,
    onPrimary = DeepSpaceNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CoralError
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricCyan,
    secondary = MintGreen,
    tertiary = AmberWarning,
    background = DeepSpaceNavy, // Keep engineering dark as default
    surface = SurfaceSlate,
    onPrimary = DeepSpaceNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun INTELLINAVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepSpaceNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
