package com.gowtham.esp32connect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * PermissionHandler manages runtime permissions for ESP32 Connect.
 *
 * This object centralizes permission management for:
 * - Wi-Fi scanning and connection (requires location permissions)
 * - Bluetooth Low Energy operations (requires Bluetooth and location permissions)
 * - File access for reading selected files
 *
 * Handles different permission requirements across Android API levels,
 * particularly the changes introduced in Android 12 (API 31) and Android 13 (API 33).
 */

object PermissionHandler {

    /**
     * Returns all permissions required by the ESP32 Connect application.
     *
     * Combines Wi-Fi, Bluetooth, and file access permissions based on
     * the current Android API level.
     *
     * @return List of permission strings to request
     */
    fun getAllRequiredPermissions(): List<String> {
        val permissions = mutableSetOf<String>()

        // Add Wi-Fi related permissions
        permissions.addAll(getWifiPermissions())

        // Add Bluetooth related permissions (varies by API level)
        permissions.addAll(getBluetoothPermissions())

        // Add file access permissions (varies by API level)
        permissions.addAll(getFilePermissions())

        return permissions.toList()
    }

    /**
     * Returns permissions required for Wi-Fi operations.
     *
     * Wi-Fi scanning and connection requires location permissions
     * in addition to basic Wi-Fi state permissions.
     * Android 13+ also requires NEARBY_WIFI_DEVICES permission.
     */
    private fun getWifiPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,      // Read Wi-Fi state
            Manifest.permission.CHANGE_WIFI_STATE,      // Modify Wi-Fi state
            Manifest.permission.ACCESS_NETWORK_STATE,   // Read network state
            Manifest.permission.CHANGE_NETWORK_STATE,   // Modify network state
            Manifest.permission.ACCESS_FINE_LOCATION,   // Required for Wi-Fi scanning
            Manifest.permission.ACCESS_COARSE_LOCATION  // Alternative location permission
        )
        
        // Add NEARBY_WIFI_DEVICES for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        
        return permissions
    }

    /**
     * Returns permissions required for Bluetooth Low Energy operations.
     *
     * Permission requirements changed significantly in Android 12 (API 31):
     * - Pre-Android 12: Uses legacy BLUETOOTH and BLUETOOTH_ADMIN
     * - Android 12+: Uses new granular permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
     *
     * Location permissions are required for BLE scanning on all API levels.
     */
    private fun getBluetoothPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ - use new granular Bluetooth permissions
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,        // Scan for BLE devices
                    Manifest.permission.BLUETOOTH_CONNECT,     // Connect to BLE devices
                    Manifest.permission.BLUETOOTH_ADVERTISE    // BLE advertising (optional)
                )
            )
            // Location permissions still required for BLE scanning
            permissions.addAll(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Pre-Android 12 - use legacy Bluetooth permissions
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,             // Basic Bluetooth access
                    Manifest.permission.BLUETOOTH_ADMIN,       // Bluetooth administration
                    Manifest.permission.ACCESS_FINE_LOCATION,  // Required for BLE scanning
                    Manifest.permission.ACCESS_COARSE_LOCATION // Alternative location permission
                )
            )
        }

        return permissions
    }

    /**
     * Returns permissions required for file access.
     *
     * Permission requirements changed in Android 13 (API 33):
     * - Pre-Android 13: Uses broad READ_EXTERNAL_STORAGE
     * - Android 13+: Uses granular media permissions (READ_MEDIA_IMAGES)
     */
    private fun getFilePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - use granular media permissions for better privacy
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Pre-Android 13 - use legacy storage permission
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Checks if all Wi-Fi related permissions are granted.
     *
     * @param context Android context for permission checking
     * @return true if all Wi-Fi permissions are granted
     */
    fun hasWifiPermissions(context: Context): Boolean {
        return getWifiPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if all Bluetooth related permissions are granted.
     *
     * @param context Android context for permission checking
     * @return true if all Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return getBluetoothPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if all file access permissions are granted.
     *
     * @param context Android context for permission checking
     * @return true if all file access permissions are granted
     */
    private fun hasFilePermissions(context: Context): Boolean {
        return getFilePermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if all required permissions are granted.
     *
     * @param context Android context for permission checking
     * @return true if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasWifiPermissions(context) &&
                hasBluetoothPermissions(context) &&
                hasFilePermissions(context)
    }
}