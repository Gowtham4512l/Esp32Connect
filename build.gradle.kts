/**
 * Top-level build configuration for ESP32 Connect project.
 *
 * This Gradle build script defines:
 * - Project-wide plugin configurations
 * - Common settings applied to all modules
 * - Plugin versions managed through version catalog
 *
 * All plugins are applied with 'apply false' to make them available
 * to sub-modules without applying them at the project level.
 */

plugins {
    // Android application plugin (applied to app module)
    alias(libs.plugins.android.application) apply false

    // Kotlin Android plugin (applied to app module) 
    alias(libs.plugins.kotlin.android) apply false

    // Kotlin Parcelize plugin for Parcelable code generation
    alias(libs.plugins.kotlin.parcelize) apply false

    // Kotlin serialization plugin for JSON handling
    alias(libs.plugins.kotlin.serialization) apply false
}