package com.yuranime.dinz.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ColorAccentPrimary,
    onPrimary = ColorTextPrimary,
    secondary = ColorMetricFavorites,
    onSecondary = ColorBackgroundPrimary,
    background = ColorBackgroundPrimary,
    onBackground = ColorTextPrimary,
    surface = ColorSurface,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorControlCircle,
    onSurfaceVariant = ColorTextSecondary
)

@Composable
fun YuranimeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = ColorBackgroundPrimary.toArgb()
            window.navigationBarColor = ColorBackgroundPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun AnimeInTheme(
    content: @Composable () -> Unit
) = YuranimeTheme(content = content)
