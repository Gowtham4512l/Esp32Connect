/**
 * Settings configuration for ESP32 Connect project.
 * 
 * This file configures:
 * - Plugin repositories and resolution
 * - Dependency repositories
 * - Project structure and module inclusion
 * - Repository filtering for security and performance
 */

// === PLUGIN MANAGEMENT ===
// Configure repositories for Gradle plugins
pluginManagement {
    repositories {
        // Google's Maven repository (priority for Android plugins)
        google {
            content {
                // Restrict to Android and Google-specific artifacts for security
                includeGroupByRegex("com\\.android.*")     // Android build tools
                includeGroupByRegex("com\\.google.*")      // Google libraries
                includeGroupByRegex("androidx.*")           // AndroidX libraries
            }
        }
        mavenCentral()          // Central Maven repository for general dependencies
        gradlePluginPortal()    // Gradle's official plugin repository
    }
}

// === DEPENDENCY RESOLUTION ===
// Configure repositories for project dependencies
dependencyResolutionManagement {
    // Enforce centralized repository management (security best practice)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()        // Google's Maven repository for Android libraries
        mavenCentral()  // Central Maven repository for open-source libraries
    }
}

// === PROJECT STRUCTURE ===
rootProject.name = "Esp32 Connect"  // Project display name
include(":app")                     // Include the main application module
 