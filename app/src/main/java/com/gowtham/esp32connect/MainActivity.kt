package com.gowtham.esp32connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.gowtham.esp32connect.ui.theme.ESPConnectTheme

/**
 * MainActivity is the main entry point for the ESP32 Connect application.
 *
 * This activity sets up the Compose UI framework and provides the main
 * application scaffold. It enables edge-to-edge display for a modern
 * Android experience and applies the custom ESP Connect theme.
 */

class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is starting.
     * Sets up the Compose UI with edge-to-edge display and theme.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern Android experience
        enableEdgeToEdge()

        // Set up Compose UI content
        setContent {
            // Apply custom ESP Connect theme
            ESPConnectTheme {
                // Main app scaffold with proper padding handling
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    // Load the main application screen
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}