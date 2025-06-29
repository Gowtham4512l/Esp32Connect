package com.gowtham.esp32connect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography configuration for ESP32 Connect application.
 *
 * This file defines the Material 3 typography scale which provides:
 * - Consistent text styling across the application
 * - Proper font sizes, weights, and spacing for different UI elements
 * - Accessibility-compliant text sizing and contrast
 * - Responsive typography that scales appropriately
 *
 * The typography follows Material Design 3 guidelines with optimized
 * font weights and letter spacing for technical/IoT applications.
 */

/**
 * Material 3 Typography scale for ESP32 Connect.
 *
 * Typography hierarchy from largest to smallest:
 * - Display: Hero text, large headlines
 * - Headline: Section headers, page titles
 * - Title: Card titles, prominent text
 * - Body: Main content text
 * - Label: UI labels, captions, small text
 */
val Typography = Typography(
    // === DISPLAY STYLES ===
    // Largest text styles for hero content and major headlines

    /** Display Large - Hero text, splash screens (57sp) */
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp  // Tight spacing for large text
    ),

    /** Display Medium - Large headlines, feature text (45sp) */
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),

    /** Display Small - Medium headlines (36sp) */
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // === HEADLINE STYLES ===
    // Section headers and page-level titles

    /** Headline Large - Main page headers (32sp) */
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    /** Headline Medium - App title, major section headers (28sp) */
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,  // Slightly bolder for prominence
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    /** Headline Small - Section headers, card group titles (24sp) */
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // === TITLE STYLES ===
    // Card titles, dialog headers, prominent UI text

    /** Title Large - Card headers, dialog titles (22sp) */
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,      // Bold for strong hierarchy
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    /** Title Medium - Card subtitles, tab labels, list headers (16sp) */
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp           // Slightly spaced for readability
    ),

    /** Title Small - Small card titles, form labels (14sp) */
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // === BODY STYLES ===
    // Main content text, descriptions, and readable text

    /** Body Large - Primary content text, descriptions (16sp) */
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,                // Generous line height for readability
        letterSpacing = 0.5.sp             // Improved readability spacing
    ),

    /** Body Medium - Secondary content, card descriptions (14sp) */
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    /** Body Small - Tertiary content, supplemental text (12sp) */
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp             // More spacing for small text legibility
    ),

    // === LABEL STYLES ===
    // UI labels, buttons, captions, and functional text

    /** Label Large - Button text, prominent labels (14sp) */
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,    // Medium weight for clarity
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    /** Label Medium - Standard UI labels, tabs, chips (12sp) */
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp             // Wider spacing for UI labels
    ),

    /** Label Small - Captions, status text, badges (11sp) */
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp             // Maximum spacing for smallest text
    )
)