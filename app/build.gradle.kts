/**
 * App-level build configuration for ESP32 Connect Android application.
 *
 * This Gradle build script configures:
 * - Android application plugin with Kotlin support
 * - Build variants (debug/release)
 * - Dependencies and libraries
 * - Compile and target SDK versions
 * - Jetpack Compose configuration
 */

plugins {
    alias(libs.plugins.android.application)      // Android application plugin
    alias(libs.plugins.kotlin.android)           // Kotlin Android support
    alias(libs.plugins.kotlin.parcelize)         // Parcelable code generation
    alias(libs.plugins.kotlin.serialization)     // Kotlin serialization support
}

android {
    // === APPLICATION CONFIGURATION ===
    namespace = "com.gowtham.esp32connect"        // Package namespace
    compileSdk = 34                              // SDK version to compile against

    defaultConfig {
        applicationId = "com.gowtham.esp32connect"   // Unique app identifier
        minSdk = 24                              // Minimum Android API level (Android 7.0)
        targetSdk = 34                           // Target Android API level (Android 14)
        versionCode = 1                          // Internal version number
        versionName = "1.0"                      // User-facing version string

        // Testing configuration
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vector drawable support for older Android versions
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // === BUILD TYPES CONFIGURATION ===
    buildTypes {
        // Release build configuration
        release {
            isMinifyEnabled = false              // Code shrinking disabled
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"             // App-specific ProGuard rules
            )
        }

        // Debug build configuration
        debug {
            isMinifyEnabled = false              // No code shrinking for debugging
            isDebuggable = true                  // Enable debugging features
        }
    }

    // === COMPILATION OPTIONS ===
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8   // Java 8 source compatibility
        targetCompatibility = JavaVersion.VERSION_1_8   // Java 8 target compatibility
    }

    kotlinOptions {
        jvmTarget = "1.8"                               // Kotlin JVM target version
    }

    // === JETPACK COMPOSE CONFIGURATION ===
    buildFeatures {
        compose = true                                  // Enable Jetpack Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"       // Compose compiler version
    }

    // === PACKAGING OPTIONS ===
    packaging {
        resources {
            // Exclude license files to avoid conflicts
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// === DEPENDENCIES CONFIGURATION ===
dependencies {
    // === CORE ANDROID LIBRARIES ===
    implementation(libs.androidx.core.ktx)                      // Kotlin extensions for Android
    implementation(libs.androidx.lifecycle.runtime.ktx)        // Lifecycle-aware components
    implementation(libs.androidx.activity.compose)             // Activity integration with Compose

    // === JETPACK COMPOSE ===
    // Compose BOM (Bill of Materials) ensures compatible versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)                           // Core Compose UI
    implementation(libs.androidx.ui.graphics)                  // Graphics and drawing APIs
    implementation(libs.androidx.ui.tooling.preview)          // Preview support in IDE
    implementation(libs.androidx.material3)                   // Material 3 design components
    implementation(libs.androidx.compose.material.icons.extended) // Extended icon set

    // === ARCHITECTURE COMPONENTS ===
    implementation(libs.androidx.lifecycle.viewmodel.compose) // ViewModel integration with Compose

    // === PERMISSIONS HANDLING ===
    implementation(libs.accompanist.permissions)              // Runtime permissions helper

    // === NETWORKING ===
    // OkHttp for HTTP communication with ESP32 devices
    implementation(libs.okhttp)                               // HTTP client
    implementation(libs.okhttp.logging.interceptor)          // Request/response logging

    // === CONCURRENCY ===
    implementation(libs.kotlinx.coroutines.android)          // Coroutines for Android

    // === DATA SERIALIZATION ===
    implementation(libs.kotlinx.serialization.json)          // JSON parsing and serialization

    // === TESTING DEPENDENCIES ===
    // Unit testing
    testImplementation(libs.junit)                           // JUnit 4 testing framework

    // Android instrumented testing
    androidTestImplementation(libs.androidx.junit)           // AndroidX JUnit extensions
    androidTestImplementation(libs.androidx.espresso.core)   // UI testing framework
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Compose testing BOM
    androidTestImplementation(libs.androidx.ui.test.junit4)  // Compose UI testing

    // Debug tools
    debugImplementation(libs.androidx.ui.tooling)            // Compose UI debugging tools
    debugImplementation(libs.androidx.ui.test.manifest)      // Test manifest for debugging
}