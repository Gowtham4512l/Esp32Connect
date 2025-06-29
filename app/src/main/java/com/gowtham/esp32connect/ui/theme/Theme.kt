package com.gowtham.esp32connect.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Theme.kt defines the Material 3 theme configuration for ESP32 Connect.
 *
 * This file provides:
 * - Light and dark color schemes using Material 3 design tokens
 * - Dynamic color support for Android 12+ devices
 * - Status bar color management
 * - Typography integration
 *
 * The theme supports both static color schemes and dynamic theming
 * that adapts to the user's wallpaper colors on supported devices.
 */

/**
 * Dark color scheme for ESP32 Connect using Material 3 design tokens.
 *
 * Uses the 80-tone colors from the Material color palette, which provide
 * appropriate contrast and accessibility for dark theme interfaces.
 * The color scheme follows Material Design 3 guidelines for dark themes.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors - main brand colors for dark theme
    primary = Primary80,
    onPrimary = OnPrimary80,
    primaryContainer = PrimaryContainer80,
    onPrimaryContainer = OnPrimaryContainer80,

    // Secondary colors - supporting UI elements for dark theme
    secondary = Secondary80,
    onSecondary = OnSecondary80,
    secondaryContainer = SecondaryContainer80,
    onSecondaryContainer = OnSecondaryContainer80,

    // Tertiary colors - additional accent colors for dark theme
    tertiary = Tertiary80,
    onTertiary = OnTertiary80,
    tertiaryContainer = TertiaryContainer80,
    onTertiaryContainer = OnTertiaryContainer80,

    // Error colors - for error states in dark theme
    error = Error80,
    onError = OnError80,
    errorContainer = ErrorContainer80,
    onErrorContainer = OnErrorContainer80,

    // Surface and background colors for dark theme
    background = Background80,
    onBackground = OnBackground80,
    surface = Surface80,
    onSurface = OnSurface80,
    surfaceVariant = SurfaceVariant80,
    onSurfaceVariant = OnSurfaceVariant80,

    // Outline colors for borders and dividers in dark theme
    outline = Outline80,
    outlineVariant = OutlineVariant80,
)

/**
 * Light color scheme for ESP32 Connect using Material 3 design tokens.
 *
 * Uses the 40-tone colors from the Material color palette, which provide
 * appropriate contrast and accessibility for light theme interfaces.
 * The color scheme follows Material Design 3 guidelines for light themes.
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors - main brand colors for light theme
    primary = Primary40,
    onPrimary = OnPrimary40,
    primaryContainer = PrimaryContainer40,
    onPrimaryContainer = OnPrimaryContainer40,

    // Secondary colors - supporting UI elements for light theme
    secondary = Secondary40,
    onSecondary = OnSecondary40,
    secondaryContainer = SecondaryContainer40,
    onSecondaryContainer = OnSecondaryContainer40,

    // Tertiary colors - additional accent colors for light theme
    tertiary = Tertiary40,
    onTertiary = OnTertiary40,
    tertiaryContainer = TertiaryContainer40,
    onTertiaryContainer = OnTertiaryContainer40,

    // Error colors - for error states in light theme
    error = Error40,
    onError = OnError40,
    errorContainer = ErrorContainer40,
    onErrorContainer = OnErrorContainer40,

    // Surface and background colors for light theme
    background = Background40,
    onBackground = OnBackground40,
    surface = Surface40,
    onSurface = OnSurface40,
    surfaceVariant = SurfaceVariant40,
    onSurfaceVariant = OnSurfaceVariant40,

    // Outline colors for borders and dividers in light theme
    outline = Outline40,
    outlineVariant = OutlineVariant40,
)

/**
 * Main theme composable for ESP32 Connect application.
 *
 * Features:
 * - Automatic dark/light theme detection based on system settings
 * - Dynamic color support for Android 12+ (Material You)
 * - Custom color schemes for older Android versions
 * - Status bar color management
 * - Typography integration
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic colors on Android 12+ (defaults to true)
 * @param content The composable content to theme
 */
@Composable
fun ESPConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (Material You)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // === COLOR SCHEME SELECTION ===
    // Choose appropriate color scheme based on Android version and user preferences
    val colorScheme = when {
        // Android 12+ with dynamic color support (Material You)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)  // Colors from user's wallpaper (dark)
            } else {
                dynamicLightColorScheme(context) // Colors from user's wallpaper (light)
            }
        }

        // Use custom dark color scheme
        darkTheme -> DarkColorScheme

        // Use custom light color scheme (default)
        else -> LightColorScheme
    }

    // === STATUS BAR STYLING ===
    // Configure status bar appearance to match the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match primary color
            window.statusBarColor = colorScheme.primary.toArgb()
            // Configure status bar content color (light/dark icons and text)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // === APPLY MATERIAL THEME ===
    // Apply the selected color scheme and typography to the content
    MaterialTheme(
        colorScheme = colorScheme,  // Selected color scheme (light/dark/dynamic)
        typography = Typography,     // Custom typography from Type.kt
        content = content           // The app's composable content
    )
}