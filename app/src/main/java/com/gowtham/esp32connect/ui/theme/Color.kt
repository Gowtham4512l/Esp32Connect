package com.gowtham.esp32connect.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for ESP32 Connect application.
 *
 * This file defines the Material 3 color scheme for both light and dark themes,
 * along with custom colors specific to ESP32 connectivity features.
 *
 * Color naming convention follows Material 3 guidelines:
 * - Numbers (40, 80) represent the tone value in the Material color system
 * - 40: Colors for light theme
 * - 80: Colors for dark theme
 */

// === LIGHT THEME COLORS ===
// Primary colors - main brand colors for the app
val Primary40 = Color(0xFF6B4EFF)          // Main purple for buttons and highlights
val PrimaryContainer40 = Color(0xFFE6DEFF)  // Light purple backgrounds
val OnPrimary40 = Color(0xFFFFFFFF)         // Text on primary color
val OnPrimaryContainer40 = Color(0xFF21005D) // Text on primary container

// Secondary colors - supporting UI elements
val Secondary40 = Color(0xFF625B71)         // Secondary actions and accents
val SecondaryContainer40 = Color(0xFFE8DEF8) // Secondary backgrounds
val OnSecondary40 = Color(0xFFFFFFFF)       // Text on secondary
val OnSecondaryContainer40 = Color(0xFF1E192B) // Text on secondary container

// Tertiary colors - additional accent colors
val Tertiary40 = Color(0xFF7D5260)          // Tertiary UI elements
val TertiaryContainer40 = Color(0xFFFFD8E4)  // Tertiary backgrounds
val OnTertiary40 = Color(0xFFFFFFFF)        // Text on tertiary
val OnTertiaryContainer40 = Color(0xFF31111D) // Text on tertiary container

// Error colors - for error states and warnings
val Error40 = Color(0xFFBA1A1A)             // Error text and icons
val ErrorContainer40 = Color(0xFFFFDAD6)     // Error message backgrounds
val OnError40 = Color(0xFFFFFFFF)           // Text on error color
val OnErrorContainer40 = Color(0xFF410002)   // Text on error container

// Surface and background colors
val Background40 = Color(0xFFFFFBFE)        // Main app background
val OnBackground40 = Color(0xFF1C1B1F)      // Text on background
val Surface40 = Color(0xFFFFFBFE)           // Card and surface backgrounds
val OnSurface40 = Color(0xFF1C1B1F)         // Text on surfaces
val SurfaceVariant40 = Color(0xFFE7E0EC)    // Alternative surface color
val OnSurfaceVariant40 = Color(0xFF49454F)  // Text on surface variant

// Outline colors - for borders and dividers
val Outline40 = Color(0xFF79747E)           // Primary outline color
val OutlineVariant40 = Color(0xFFCAC4D0)    // Secondary outline color

// === DARK THEME COLORS ===
// Primary colors for dark theme
val Primary80 = Color(0xFFCFBCFF)           // Main purple for dark theme
val PrimaryContainer80 = Color(0xFF4F378B)   // Dark purple backgrounds
val OnPrimary80 = Color(0xFF371E73)         // Text on primary in dark theme
val OnPrimaryContainer80 = Color(0xFFE6DEFF) // Text on primary container in dark

// Secondary colors for dark theme
val Secondary80 = Color(0xFFCCC2DC)         // Secondary elements in dark theme
val SecondaryContainer80 = Color(0xFF4A4458) // Dark secondary backgrounds
val OnSecondary80 = Color(0xFF332D41)       // Text on secondary in dark
val OnSecondaryContainer80 = Color(0xFFE8DEF8) // Text on secondary container

// Tertiary colors for dark theme
val Tertiary80 = Color(0xFFEFB8C8)          // Tertiary elements in dark theme
val TertiaryContainer80 = Color(0xFF633B48)  // Dark tertiary backgrounds
val OnTertiary80 = Color(0xFF492532)        // Text on tertiary in dark
val OnTertiaryContainer80 = Color(0xFFFFD8E4) // Text on tertiary container

// Error colors for dark theme
val Error80 = Color(0xFFFFB4AB)             // Error elements in dark theme
val ErrorContainer80 = Color(0xFF93000A)     // Dark error backgrounds
val OnError80 = Color(0xFF690005)           // Text on error in dark
val OnErrorContainer80 = Color(0xFFFFDAD6)   // Text on error container

// Surface and background colors for dark theme
val Background80 = Color(0xFF10101A)        // Dark app background
val OnBackground80 = Color(0xFFE6E1E5)      // Light text on dark background
val Surface80 = Color(0xFF10101A)           // Dark surface backgrounds
val OnSurface80 = Color(0xFFE6E1E5)         // Light text on dark surfaces
val SurfaceVariant80 = Color(0xFF49454F)    // Alternative dark surface
val OnSurfaceVariant80 = Color(0xFFCAC4D0)  // Text on dark surface variant

// Outline colors for dark theme
val Outline80 = Color(0xFF938F99)           // Primary outline in dark theme
val OutlineVariant80 = Color(0xFF49454F)    // Secondary outline in dark theme

// === CUSTOM ESP32 CONNECT COLORS ===
// Brand and feature-specific colors for the application

// ESP32 branding colors
val ESP32Blue = Color(0xFF2196F3)           // Primary ESP32 brand color
val ESP32BlueVariant = Color(0xFF1976D2)     // Darker variant for emphasis

// Connectivity technology colors
val BluetoothBlue = Color(0xFF0277BD)       // Bluetooth LE indicators
val WifiGreen = Color(0xFF4CAF50)           // Wi-Fi connectivity indicators
val ConnectedGreen = Color(0xFF388E3C)      // Success/connected state color

// Transfer and progress colors
val TransferOrange = Color(0xFFFF9800)      // Transfer button and progress
val ProgressBlue = Color(0xFF2196F3)        // Progress indicators

// Additional semantic colors for enhanced UI feedback
val WarningAmber = Color(0xFFFF6F00)        // Warning states and alerts
val InfoCyan = Color(0xFF00BCD4)            // Informational messages
val DisabledGray = Color(0xFF9E9E9E)        // Disabled states and inactive elements
val SuccessGreen = Color(0xFF4CAF50)        // Success states and confirmations
val DangerRed = Color(0xFFF44336)           // Error states and critical alerts