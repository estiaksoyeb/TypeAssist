package com.typeassist.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material 3 Light Theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo 100
    onPrimaryContainer = Color(0xFF312E81), // Indigo 900
    
    secondary = Color(0xFF059669), // Emerald 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5), // Emerald 100
    onSecondaryContainer = Color(0xFF064E3B), // Emerald 900
    
    tertiary = Color(0xFFEA580C), // Orange 600
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5), // Orange 100
    onTertiaryContainer = Color(0xFF7C2D12), // Orange 900
    
    background = Color(0xFFFFFBFE), // Off-white
    onBackground = Color(0xFF1C1B1F), // Dark Grey
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// Material 3 Dark Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF1E1B4B), // Indigo 950
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    
    secondary = Color(0xFF34D399), // Emerald 400
    onSecondary = Color(0xFF022C22), // Emerald 950
    secondaryContainer = Color(0xFF064E3B), // Emerald 900
    onSecondaryContainer = Color(0xFFD1FAE5), // Emerald 100
    
    tertiary = Color(0xFFFB923C), // Orange 400
    onTertiary = Color(0xFF431407), // Orange 950
    tertiaryContainer = Color(0xFF7C2D12), // Orange 900
    onTertiaryContainer = Color(0xFFFFEDD5), // Orange 100
    
    background = Color(0xFF1C1B1F), // Dark Grey
    onBackground = Color(0xFFE6E1E5), // Light Grey
    
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is intentionally disabled for consistency
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar follows background/surface color
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Light status bar icons if background is light (i.e. not dark theme)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}