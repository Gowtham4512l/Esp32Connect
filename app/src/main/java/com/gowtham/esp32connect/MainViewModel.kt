package com.gowtham.esp32connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.net.wifi.ScanResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainViewModel handles the core business logic for ESP32 Connect app.
 *
 * This ViewModel manages:
 * - File selection and validation
 * - Wi-Fi and BLE device scanning
 * - Device connection management
 * - File transfer operations via both Wi-Fi and BLE
 * - UI state management and error handling
 *
 * The ViewModel follows MVVM architecture pattern and uses Compose state
 * management for reactive UI updates.
 */

class MainViewModel : ViewModel() {

    // === FILE SELECTION STATE ===
    // These properties track the currently selected file for transfer

    /** Name of the selected file */
    var selectedFileName by mutableStateOf("")
        private set

    /** Human-readable size of the selected file (e.g., "4.1 MB") */
    var selectedFileSize by mutableStateOf("")
        private set

    /** URI reference to the selected file */
    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set

    // === DEVICE SCANNING STATE ===
    // These properties manage device discovery and scanning operations

    /** Whether a device scan is currently in progress */
    var isScanning by mutableStateOf(false)
        private set

    /** List of discovered Wi-Fi networks with ESP32 devices */
    var wifiDevices by mutableStateOf<List<ScanResult>>(emptyList())
        private set

    /** List of discovered Bluetooth Low Energy ESP32 devices */
    var bleDevices by mutableStateOf<List<BluetoothDevice>>(emptyList())
        private set

    // === CONNECTION STATE ===
    // Tracks the currently connected device

    /**
     * Identifier of the currently connected device.
     * For Wi-Fi: SSID of the network
     * For BLE: MAC address of the device
     */
    var connectedDevice by mutableStateOf<String?>(null)
        private set

    // === TRANSFER STATE ===
    // Manages file transfer progress and status

    /** Whether a file transfer is currently in progress */
    var isTransferring by mutableStateOf(false)
        private set

    /** Transfer progress as a float between 0.0 and 1.0 */
    var transferProgress by mutableStateOf(0f)
        private set

    // === UI MESSAGING ===
    // Handles user feedback and error messages

    /** Current error or status message to display to user */
    var errorMessage by mutableStateOf("")
        private set

    // === TRANSFER HANDLERS ===
    // Service classes that handle actual communication with ESP32 devices

    /** Handler for Wi-Fi based file transfers */
    private var wifiTransfer: WifiTransfer? = null

    /** Handler for Bluetooth Low Energy file transfers */
    private var bleTransfer: BleTransfer? = null

    /** Application context needed for system services */
    private var appContext: Context? = null

    // === Progress throttling ===
    private var lastProgressUpdate = 0L
    private val progressUpdateInterval = 100L // Update UI every 100ms max

    /**
     * Initializes the ViewModel with Android context and creates transfer handlers.
     * This must be called before any device scanning or transfer operations.
     *
     * @param context Android context (will be converted to application context)
     */
    fun initializeWithContext(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            appContext = context.applicationContext
            wifiTransfer = WifiTransfer(appContext!!)
            bleTransfer = BleTransfer(appContext!!)
        }
    }

    /**
     * Handles file selection from the device storage.
     * Extracts file metadata and validates file size.
     *
     * @param context Android context for content resolution
     * @param uri URI of the selected file
     */
    fun selectFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (appContext == null) {
                    withContext(Dispatchers.Main) {
                        initializeWithContext(context)
                    }
                    // Wait for initialization
                    delay(100)
                }

                selectedFileUri = uri
                withContext(Dispatchers.Main) { clearError() }

                // Extract file metadata using ContentResolver
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)

                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex =
                            it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)

                        var fileName = "Unknown"
                        var fileSize = ""

                        // Extract file name
                        if (nameIndex >= 0) {
                            fileName = it.getString(nameIndex) ?: "Unknown"
                        }

                        if (sizeIndex >= 0) {
                            val size = it.getLong(sizeIndex)
                            fileSize = formatFileSize(size)
                        }

                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            selectedFileName = fileName
                            selectedFileSize = fileSize

                            // Warn user about large files that may take longer to transfer
                            val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                            if (size > 10 * 1024 * 1024) { // // 10MB threshold
                                showError("Large file selected ($fileSize). Transfer may take longer.")
                            } else {
                                showError("File selected successfully: $fileName")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error selecting file: ${e.message}")
                }
            }
        }
    }

    /**
     * Initiates scanning for ESP32 devices advertising over Wi-Fi.
     * Searches for networks with ESP32-related SSIDs and updates the device list.
     */
    fun scanWifiDevices() {
        if (isScanning || appContext == null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isScanning = true
                    clearError()
                    showError("Scanning for ESP32 Wi-Fi networks...")
                }

                val devices = wifiTransfer?.scanForESP32Devices() ?: emptyList()

                withContext(Dispatchers.Main) {
                    wifiDevices = devices
                    if (devices.isEmpty()) {
                        showError("No ESP32 Wi-Fi networks found. Check if ESP32 is in AP mode.")
                    } else {
                        showError("Found ${devices.size} ESP32 device(s)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    wifiDevices = emptyList()
                    showError("Wi-Fi scan failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isScanning = false
                }
            }
        }
    }

    /**
     * Initiates scanning for ESP32 devices advertising over Bluetooth Low Energy.
     * Looks for devices with ESP32-related names and compatible GATT services.
     */
    fun scanBleDevices() {
        if (isScanning || appContext == null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isScanning = true
                    clearError()
                    showError("Scanning for ESP32 BLE devices...")
                }

                val devices = bleTransfer?.scanForESP32Devices() ?: emptyList()

                withContext(Dispatchers.Main) {
                    bleDevices = devices
                    if (devices.isEmpty()) {
                        showError("No ESP32 BLE devices found. Check if ESP32 is advertising.")
                    } else {
                        showError("Found ${devices.size} ESP32 BLE device(s)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    bleDevices = emptyList()
                    showError("BLE scan failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isScanning = false
                }
            }
        }
    }

    /**
     * Attempts to connect to a Wi-Fi ESP32 device.
     * Automatically disconnects from any existing BLE connection.
     *
     * @param device The Wi-Fi scan result representing the ESP32 device
     */
    fun connectToWifiDevice(device: ScanResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    clearError()
                    showError("Connecting to ${device.SSID}...")
                }

                val success = wifiTransfer?.connectToDevice(device) ?: false

                withContext(Dispatchers.Main) {
                    if (success) {
                        connectedDevice = device.SSID
                        showError("Connected to ${device.SSID} successfully!")
                    } else {
                        showError("Failed to connect to ${device.SSID}. Check password and try again.")
                    }
                }

                if (success) {
                    // Cleanup BLE connection in background
                    // Ensure only one connection type is active at a time
                    bleTransfer?.cleanup()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Connection error: ${e.message}")
                }
            }
        }
    }

    /**
     * Attempts to connect to a Bluetooth Low Energy ESP32 device.
     * Automatically disconnects from any existing Wi-Fi connection.
     *
     * @param device The Bluetooth device representing the ESP32
     */
    @SuppressLint("MissingPermission")
    fun connectToBleDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    clearError()
                    showError("Connecting to ${device.name ?: device.address}...")
                }

                val success = bleTransfer?.connectToDevice(device) ?: false

                withContext(Dispatchers.Main) {
                    if (success) {
                        connectedDevice = device.address
                        showError("Connected to ${device.name ?: device.address} successfully!")
                    } else {
                        showError("Failed to connect to ${device.name ?: "BLE device"}. Check device status.")
                    }
                }

                if (success) {
                    // Cleanup Wi-Fi connection in background
                    // Ensure only one connection type is active at a time
                    wifiTransfer?.cleanup()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("BLE connection error: ${e.message}")
                }
            }
        }
    }

    /**
     * Initiates file transfer via Wi-Fi to the connected ESP32 device.
     * Uses HTTP POST to upload the file to the ESP32's web server.
     *
     * Prerequisites:
     * - File must be selected
     * - Device must be connected via Wi-Fi
     * - No other transfer in progress
     */
    fun transferFileViaWifi() {
        if (selectedFileUri == null || connectedDevice == null || isTransferring) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isTransferring = true
                    transferProgress = 0f
                    clearError()
                    showError("Starting Wi-Fi transfer...")
                }

                wifiTransfer?.transferFile(
                    selectedFileUri!!,
                    onProgress = { progress ->
                        // Throttle progress updates to prevent UI flooding
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressUpdate >= progressUpdateInterval) {
                            lastProgressUpdate = currentTime

                            // Update UI with progress percentage
                            viewModelScope.launch(Dispatchers.Main) {
                                transferProgress = progress
                                if (progress > 0f && progress < 1f) {
                                    showError("Transferring... ${(progress * 100).toInt()}%")
                                }
                            }
                        }
                    },
                    onComplete = { success ->
                        viewModelScope.launch(Dispatchers.Main) {
                            isTransferring = false
                            if (success) {
                                transferProgress = 1f
                                showError("File transferred successfully via Wi-Fi!")
                            } else {
                                transferProgress = 0f
                                showError("Wi-Fi transfer failed. Check ESP32 connection and try again.")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isTransferring = false
                    transferProgress = 0f
                    showError("Transfer error: ${e.message}")
                }
            }
        }
    }

    /**
     * Initiates file transfer via Bluetooth Low Energy to the connected ESP32 device.
     * Sends the file in chunks over GATT characteristics.
     *
     * Prerequisites:
     * - File must be selected
     * - Device must be connected via BLE
     * - No other transfer in progress
     */
    fun transferFileViaBle() {
        if (selectedFileUri == null || connectedDevice == null || isTransferring) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isTransferring = true
                    transferProgress = 0f
                    clearError()
                    showError("Starting BLE transfer...")
                }

                bleTransfer?.transferFile(
                    selectedFileUri!!,
                    onProgress = { progress ->
                        // Throttle progress updates to prevent UI flooding
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressUpdate >= progressUpdateInterval) {
                            lastProgressUpdate = currentTime

                            // Update UI with progress percentage
                            viewModelScope.launch(Dispatchers.Main) {
                                transferProgress = progress
                                if (progress > 0f && progress < 1f) {
                                    showError("Transferring... ${(progress * 100).toInt()}%")
                                }
                            }
                        }
                    },
                    onComplete = { success ->
                        viewModelScope.launch(Dispatchers.Main) {
                            isTransferring = false
                            if (success) {
                                transferProgress = 1f
                                showError("File transferred successfully via BLE!")
                            } else {
                                transferProgress = 0f
                                showError("BLE transfer failed. Check connection and try again.")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isTransferring = false
                    transferProgress = 0f
                    showError("Transfer error: ${e.message}")
                }
            }
        }
    }

    /**
     * Cancels any ongoing file transfer operation.
     * Resets transfer state and notifies both Wi-Fi and BLE handlers.
     */
    fun cancelTransfer() {
        viewModelScope.launch(Dispatchers.IO) {
            wifiTransfer?.cancelTransfer()
            bleTransfer?.cancelTransfer()

            withContext(Dispatchers.Main) {
                isTransferring = false
                transferProgress = 0f
                showError("Transfer cancelled by user")
            }
        }
    }

    /**
     * Displays a message to the user with automatic dismissal.
     * Success messages are cleared after 3 seconds, errors after 5 seconds.
     *
     * @param message The message to display (empty string to clear immediately)
     */
    fun showError(message: String) {
        if (message.isBlank()) {
            errorMessage = ""
            return
        }

        errorMessage = message

        // Determine auto-clear delay based on message type
        val clearDelay = if (message.contains("successfully", ignoreCase = true) ||
            message.contains("Connected to", ignoreCase = true) ||
            message.contains("Found", ignoreCase = true) ||
            message.contains("selected", ignoreCase = true)
        ) {
            3000L // Success messages: 3 seconds
        } else {
            5000L // Error messages: 5 seconds
        }

        // Schedule automatic message clearing
        // Use Dispatchers.Main.immediate for time-sensitive UI updates
        viewModelScope.launch(Dispatchers.Main.immediate) {
            delay(clearDelay)
            // Only clear if message hasn't changed
            if (errorMessage == message) {
                clearError()
            }
        }
    }

    /**
     * Clears the current error/status message.
     */
    private fun clearError() {
        errorMessage = ""
    }

    /**
     * Converts file size in bytes to human-readable format.
     *
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "4.1 MB", "256 KB", "1024 bytes")
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }

    /**
     * Called when the ViewModel is destroyed.
     * Cleans up transfer handlers and closes connections.
     */
    override fun onCleared() {
        super.onCleared()
        // Cleanup in background to avoid blocking main thread
        viewModelScope.launch(Dispatchers.IO) {
            wifiTransfer?.cleanup()
            bleTransfer?.cleanup()
        }
    }
}