package com.gowtham.esp32connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * BleTransfer handles Bluetooth Low Energy communication with ESP32 devices.
 *
 * This class provides functionality to:
 * - Scan for ESP32 devices advertising over BLE
 * - Connect to ESP32 devices using GATT protocol
 * - Transfer files by chunking them over BLE characteristics
 * - Manage connection state and handle cleanup
 *
 * The implementation uses custom GATT service UUIDs that should match
 * the ESP32 firmware configuration.
 */

@SuppressLint("MissingPermission")
class BleTransfer(private val context: Context) {

    companion object {
        // === BLE SERVICE CONFIGURATION ===
        // These UUIDs must match your ESP32 firmware configuration

        /** Main GATT service UUID for ESP32 communication */
        private val ESP32_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

        /** TX characteristic UUID - for sending data to ESP32 */
        private val ESP32_CHAR_TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

        /** RX characteristic UUID - for receiving data from ESP32 */
        private val ESP32_CHAR_RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

        // === TIMING CONFIGURATION ===
        /** Maximum time to scan for BLE devices */
        private const val SCAN_PERIOD = 10000L // 10 seconds

        /** Maximum time to wait for device connection */
        private const val CONNECTION_TIMEOUT = 15000L // 15 seconds

        /** Requested MTU size for faster data transfer */
        private const val MTU_SIZE = 512

        /** Size of each data chunk sent over BLE */
        private const val CHUNK_SIZE = 500 // Bytes per BLE packet
    }

    // === BLUETOOTH SYSTEM SERVICES ===
    /** Android Bluetooth manager service */
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    /** Bluetooth adapter for device operations */
    private val bluetoothAdapter = bluetoothManager.adapter

    /** BLE scanner for discovering devices */
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    // Use background thread for BLE operations
    private val bleHandlerThread = HandlerThread("BleOperations").apply { start() }
    private val bleHandler = Handler(bleHandlerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    // === CONNECTION STATE ===
    /** Active GATT connection to ESP32 device */
    private var bluetoothGatt: BluetoothGatt? = null

    /** TX characteristic for sending data to ESP32 */
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    /** RX characteristic for receiving data from ESP32 */
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    /** Flag to cancel ongoing file transfers */
    private var isTransferCancelled = false

    /** Thread-safe connection status */
    private val isConnected = AtomicBoolean(false)

    /**
     * Scans for ESP32 devices advertising over Bluetooth Low Energy.
     *
     * Filters devices by name (must contain "ESP32" or "ESP") and optionally
     * by service UUID if the device advertises the expected GATT service.
     *
     * @return List of discovered BluetoothDevice objects
     */
    suspend fun scanForESP32Devices(): List<BluetoothDevice> =
        withTimeoutOrNull(SCAN_PERIOD + 1000) {
            suspendCancellableCoroutine { continuation ->
                // Check if Bluetooth is enabled and scanner is available
                if (bluetoothAdapter?.isEnabled != true || bluetoothLeScanner == null) {
                    continuation.resume(emptyList())
                    return@suspendCancellableCoroutine
                }

                val foundDevices = mutableListOf<BluetoothDevice>()
                val scanResults = mutableSetOf<String>() // To avoid duplicates

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        // Process scan results on background thread
                        bleHandler.post {
                            try {
                                val device = result.device
                                val deviceName = device?.name

                                if (device != null && deviceName != null &&
                                    (deviceName.contains("ESP32", ignoreCase = true) ||
                                            deviceName.contains("ESP", ignoreCase = true)) &&
                                    !scanResults.contains(device.address)
                                ) {
                                    synchronized(scanResults) {
                                        scanResults.add(device.address)
                                        foundDevices.add(device)
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle scanning errors silently
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        if (!continuation.isCompleted) {
                            continuation.resume(emptyList())
                        }
                    }
                }

                // Start scanning with service UUID filter
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                // Try with service filter first, fallback to general scan
                val scanFilters = listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(ESP32_SERVICE_UUID))
                        .build()
                )

                try {
                    bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)

                    // Use background handler for SCAN_PERIOD timeout
                    bleHandler.postDelayed({
                        try {
                            bluetoothLeScanner.stopScan(scanCallback)
                            if (!continuation.isCompleted) {
                                continuation.resume(foundDevices.toList())
                            }
                        } catch (e: Exception) {
                            if (!continuation.isCompleted) {
                                continuation.resume(foundDevices.toList())
                            }
                        }
                    }, SCAN_PERIOD)

                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!continuation.isCompleted) {
                        continuation.resume(emptyList())
                    }
                }

                continuation.invokeOnCancellation {
                    try {
                        bluetoothLeScanner.stopScan(scanCallback)
                    } catch (e: Exception) {
                        // Scanner might already be stopped
                    }
                }
            }
        } ?: emptyList()

    /**
     * Establishes a GATT connection to an ESP32 device.
     *
     * This method:
     * 1. Initiates GATT connection
     * 2. Requests larger MTU for faster transfers
     * 3. Discovers GATT services
     * 4. Locates TX/RX characteristics
     * 5. Enables notifications on RX characteristic
     *
     * @param device The ESP32 BluetoothDevice to connect to
     * @return true if connection and service discovery successful
     */
    suspend fun connectToDevice(device: BluetoothDevice): Boolean =
        withTimeoutOrNull(CONNECTION_TIMEOUT) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                isConnected.set(false)
                var isResumed = false

                val gattCallback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        // Handle connection state changes on background thread
                        bleHandler.post {
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    bluetoothGatt = gatt
                                    isConnected.set(true)
                                    // Request larger MTU for faster data transfer
                                    gatt?.requestMtu(MTU_SIZE)
                                }

                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    isConnected.set(false)
                                    bluetoothGatt = null
                                    if (!isResumed) {
                                        isResumed = true
                                        continuation.resume(false)
                                    }
                                }
                            }
                        }
                    }

                    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                        // MTU changed, now discover services
                        bleHandler.post {
                            gatt?.discoverServices()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        bleHandler.post {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                val service = gatt?.getService(ESP32_SERVICE_UUID)
                                if (service != null) {
                                    txCharacteristic = service.getCharacteristic(ESP32_CHAR_TX_UUID)
                                    rxCharacteristic = service.getCharacteristic(ESP32_CHAR_RX_UUID)

                                    val txChar = txCharacteristic
                                    val rxChar = rxCharacteristic

                                    if (txChar != null && rxChar != null) {
                                        // Enable notifications on RX characteristic
                                        val success =
                                            gatt.setCharacteristicNotification(rxChar, true)
                                        if (success) {
                                            val descriptor = rxChar.getDescriptor(
                                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                                            )
                                            descriptor?.let { desc ->
                                                desc.value =
                                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                                gatt.writeDescriptor(desc)
                                            }
                                        }

                                        if (!isResumed) {
                                            isResumed = true
                                            continuation.resume(true)
                                        }
                                    } else {
                                        if (!isResumed) {
                                            isResumed = true
                                            continuation.resume(false)
                                        }
                                    }
                                } else {
                                    if (!isResumed) {
                                        isResumed = true
                                        continuation.resume(false)
                                    }
                                }
                            } else {
                                if (!isResumed) {
                                    isResumed = true
                                    continuation.resume(false)
                                }
                            }
                        }
                    }

                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        // Handle write completion for file transfer
                        // This can be used for flow control if needed
                    }

                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?
                    ) {
                        // Handle responses from ESP32 during file transfer
                        if (characteristic == rxCharacteristic) {
                            val data = characteristic?.value
                            // Process acknowledgment or status from ESP32
                        }
                    }
                }

                try {
                    val gatt = device.connectGatt(context, false, gattCallback)
                    if (gatt == null && !isResumed) {
                        isResumed = true
                        continuation.resume(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(false)
                }

                continuation.invokeOnCancellation {
                    cleanup()
                }
            }
        } ?: false

    /**
     * Transfers a file to the connected ESP32 device over BLE.
     *
     * The file is sent in the following format:
     * 1. File header with size information
     * 2. File data in chunks of CHUNK_SIZE bytes
     * 3. Transfer completion signal
     *
     * @param fileUri URI of the file to transfer
     * @param onProgress Callback for transfer progress (0.0 to 1.0)
     * @param onComplete Callback for transfer completion (success/failure)
     */
    suspend fun transferFile(
        fileUri: Uri,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        if (bluetoothGatt == null || txCharacteristic == null || !isConnected.get()) {
            onComplete(false)
            return
        }

        isTransferCancelled = false

        // Perform file operations on IO dispatcher
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                inputStream?.use { stream ->
                    val fileSize = stream.available()
                    val buffer = ByteArray(CHUNK_SIZE)
                    var totalBytesSent = 0

                    // Send file header with size info
                    if (!sendFileHeader(fileSize)) {
                        onComplete(false)
                        return@withContext
                    }

                    // Send file data in chunks on IO thread
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1 &&
                        !isTransferCancelled && isConnected.get()
                    ) {
                        val chunk = if (bytesRead < CHUNK_SIZE) {
                            buffer.copyOf(bytesRead)
                        } else {
                            buffer
                        }

                        if (!sendChunk(chunk)) {
                            onComplete(false)
                            return@withContext
                        }

                        totalBytesSent += bytesRead
                        val progress = totalBytesSent.toFloat() / fileSize.toFloat()

                        // Update progress on main thread efficiently
                        mainHandler.post { onProgress(progress) }

                        // Small delay to prevent overwhelming the BLE connection
                        kotlinx.coroutines.delay(20)
                    }

                    if (!isTransferCancelled && isConnected.get()) {
                        // Send transfer completion signal
                        sendTransferComplete()
                        mainHandler.post { onComplete(true) }
                    } else {
                        mainHandler.post { onComplete(false) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { onComplete(false) }
            }
        }
    }

    /**
     * Sends file header information to ESP32.
     * Header format: "FILE_START:[fileSize]"
     */
    private suspend fun sendFileHeader(fileSize: Int): Boolean = withContext(Dispatchers.IO) {
        val header = "FILE_START:$fileSize".toByteArray()
        writeCharacteristic(header)
    }

    /**
     * Sends a chunk of file data to ESP32.
     */
    private suspend fun sendChunk(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        writeCharacteristic(data)
    }

    /**
     * Sends transfer completion signal to ESP32.
     * Signal format: "FILE_END"
     */
    private suspend fun sendTransferComplete(): Boolean = withContext(Dispatchers.IO) {
        val endSignal = "FILE_END".toByteArray()
        writeCharacteristic(endSignal)
    }

    /**
     * Writes data to the TX characteristic.
     *
     * @param data Byte array to send to ESP32
     * @return true if write operation was initiated successfully
     */
    private fun writeCharacteristic(data: ByteArray): Boolean {
        return try {
            val gatt = bluetoothGatt
            val txChar = txCharacteristic

            if (gatt != null && txChar != null && isConnected.get()) {
                // Perform write operation synchronously
                txChar.value = data
                gatt.writeCharacteristic(txChar)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cancels any ongoing file transfer operation.
     */
    fun cancelTransfer() {
        isTransferCancelled = true
    }

    /**
     * Cleans up BLE resources and closes connections.
     * Should be called when the transfer handler is no longer needed.
     */
    fun cleanup() {
        cancelTransfer()
        isConnected.set(false)

        bleHandler.post {
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            } catch (e: Exception) {
                // Handle exceptions during cleanup gracefully
            }

            // Clear all references
            bluetoothGatt = null
            txCharacteristic = null
            rxCharacteristic = null

            // Clean up background thread
            bleHandlerThread.quitSafely()
        }
    }
}