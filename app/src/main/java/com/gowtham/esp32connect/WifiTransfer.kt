package com.gowtham.esp32connect

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume

/**
 * WifiTransfer handles Wi-Fi communication with ESP32 devices.
 *
 * This class provides functionality to:
 * - Scan for ESP32 devices configured as Wi-Fi Access Points
 * - Connect to ESP32 Wi-Fi networks
 * - Transfer files via HTTP POST to ESP32 web server
 * - Handle different Android API levels for Wi-Fi connection methods
 *
 * The ESP32 device should be configured as an Access Point with:
 * - SSID containing "ESP32" or "ESP"
 * - Default IP address: 192.168.4.1
 * - HTTP server listening on /upload endpoint
 */

@SuppressLint("MissingPermission")
class WifiTransfer(private val context: Context) {

    // === ANDROID SYSTEM SERVICES ===
    /** Wi-Fi manager for network operations */
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** Connectivity manager for network state management */
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** HTTP client configured for ESP32 communication */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // === CONNECTION STATE ===
    /** Network callback for Android 10+ connection management */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Reference to the connected ESP32 network */
    private var connectedNetwork: Network? = null

    /** Flag to cancel ongoing file transfers */
    private var isTransferCancelled = false

    companion object {
        /** Maximum time to wait for Wi-Fi connection */
        private const val CONNECTION_TIMEOUT = 30000L // 30 seconds

        /** Maximum time to scan for Wi-Fi networks */
        private const val SCAN_TIMEOUT = 15000L // 15 seconds
    }

    /**
     * Scans for ESP32 devices configured as Wi-Fi Access Points.
     *
     * Filters scan results to include only networks with SSIDs containing:
     * - "ESP32" (case insensitive)
     * - "ESP" (case insensitive)
     * - Starting with "ESP32_" or "ESP_"
     *
     * @return List of ScanResult objects representing ESP32 devices
     */
    suspend fun scanForESP32Devices(): List<ScanResult> = withTimeoutOrNull(SCAN_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            val scanResultsReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: android.content.Intent?) {
                    if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                        try {
                            val scanResults = wifiManager.scanResults
                            // Filter for ESP32 devices - look for common ESP32 AP names
                            // ESP32_AP
                            val esp32Devices = scanResults?.filter { scanResult ->
                                scanResult.SSID?.let { ssid ->
                                    ssid.contains("ESP32", ignoreCase = true) ||
                                            ssid.contains("ESP", ignoreCase = true) ||
                                            ssid.startsWith("ESP32_") ||
                                            ssid.startsWith("ESP_")
                                } ?: false
                            } ?: emptyList()

                            context?.unregisterReceiver(this)
                            continuation.resume(esp32Devices)
                        } catch (e: Exception) {
                            context?.unregisterReceiver(this)
                            continuation.resume(emptyList())
                        }
                    }
                }
            }

            val intentFilter =
                android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

            try {
                context.registerReceiver(scanResultsReceiver, intentFilter)
                val scanStarted = wifiManager.startScan()

                if (!scanStarted) {
                    try {
                        context.unregisterReceiver(scanResultsReceiver)
                    } catch (e: Exception) {
                        // Receiver might not be registered
                    }
                    continuation.resume(emptyList())
                }
            } catch (e: Exception) {
                continuation.resume(emptyList())
            }

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(scanResultsReceiver)
                } catch (e: Exception) {
                    // Receiver might already be unregistered
                }
            }
        }
    } ?: emptyList()

    /**
     * Connects to an ESP32 Wi-Fi Access Point.
     *
     * Uses different connection methods based on Android API level:
     * - Android 10+: NetworkRequest API with WifiNetworkSpecifier
     * - Pre-Android 10: WifiConfiguration (deprecated but functional)
     *
     * Default ESP32 password: "esp32password" (make configurable as needed)
     *
     * @param scanResult The ESP32 network scan result
     * @return true if connection was successful
     */
    suspend fun connectToDevice(scanResult: ScanResult): Boolean =
        withTimeoutOrNull(CONNECTION_TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ - Use NetworkRequest API
                        val ssid = scanResult.SSID
                        if (ssid.isNullOrEmpty()) {
                            continuation.resume(false)
                            return@suspendCancellableCoroutine
                        }

                        val specifier = WifiNetworkSpecifier.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase("esp32password") // Default ESP32 password - make this configurable
                            .build()

                        val request = NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                            .setNetworkSpecifier(specifier)
                            .build()

                        networkCallback = object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                connectedNetwork = network
                                continuation.resume(true)
                            }

                            override fun onUnavailable() {
                                continuation.resume(false)
                            }

                            override fun onLost(network: Network) {
                                if (network == connectedNetwork) {
                                    connectedNetwork = null
                                }
                            }
                        }

                        connectivityManager.requestNetwork(request, networkCallback!!)

                    } else {
                        // Pre-Android 10 - Use WifiConfiguration (deprecated but still works)
                        @Suppress("DEPRECATION")
                        val wifiConfig = WifiConfiguration().apply {
                            SSID = "\"${scanResult.SSID}\""
                            preSharedKey = "\"esp32pass\"" // Default password
                            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                        }

                        val networkId = wifiManager.addNetwork(wifiConfig)
                        if (networkId != -1) {
                            wifiManager.disconnect()
                            val enabled = wifiManager.enableNetwork(networkId, true)
                            wifiManager.reconnect()
                            continuation.resume(enabled)
                        } else {
                            continuation.resume(false)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(false)
                }

                continuation.invokeOnCancellation {
                    networkCallback?.let {
                        try {
                            connectivityManager.unregisterNetworkCallback(it)
                        } catch (e: Exception) {
                            // Network callback might already be unregistered
                        }
                    }
                }
            }
        } ?: false

    /**
     * Transfers a file to the ESP32 device via HTTP POST.
     *
     * Process:
     * 1. Creates temporary file from URI
     * 2. Sends HTTP POST to ESP32 at 192.168.4.1/upload
     * 3. Reports progress via callback
     * 4. Cleans up temporary files
     *
     * @param fileUri URI of the file to transfer
     * @param onProgress Callback for upload progress (0.0 to 1.0)
     * @param onComplete Callback for transfer completion (success/failure)
     */
    suspend fun transferFile(
        fileUri: Uri,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        isTransferCancelled = false

        try {
            // Create temporary file from URI
            val inputStream = context.contentResolver.openInputStream(fileUri)
            val tempFile =
                File(context.cacheDir, "temp_transfer_file_${System.currentTimeMillis()}")

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Prepare the HTTP request
            val requestBody = tempFile.asRequestBody("image/*".toMediaType())

            val request = Request.Builder()
                .url("http://192.168.4.1/upload") // Default ESP32 AP IP
                .post(createProgressRequestBody(requestBody, onProgress))
                .addHeader("Content-Type", "multipart/form-data")
                .build()

            // Execute request on the connected network
            val call =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectedNetwork != null) {
                    okHttpClient.newBuilder()
                        .socketFactory(connectedNetwork!!.socketFactory)
                        .build()
                        .newCall(request)
                } else {
                    okHttpClient.newCall(request)
                }

            call.execute().use { response ->
                val success = response.isSuccessful
                onComplete(success)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false)
        } finally {
            // Clean up temp files
            try {
                val tempFiles = context.cacheDir.listFiles { file ->
                    file.name.startsWith("temp_transfer_file_")
                }
                tempFiles?.forEach { it.delete() }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Creates a RequestBody wrapper that reports upload progress.
     *
     * @param requestBody The original request body to wrap
     * @param onProgress Callback to report progress (0.0 to 1.0)
     * @return RequestBody that reports progress during upload
     */
    private fun createProgressRequestBody(
        requestBody: RequestBody,
        onProgress: (Float) -> Unit
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType() = requestBody.contentType()
            override fun contentLength() = requestBody.contentLength()

            override fun writeTo(sink: BufferedSink) {
                val totalBytes = contentLength()
                var uploadedBytes = 0L

                val progressSink = object : ForwardingSink(sink) {
                    override fun write(source: okio.Buffer, byteCount: Long) {
                        if (isTransferCancelled) {
                            throw IOException("Transfer cancelled")
                        }

                        super.write(source, byteCount)
                        uploadedBytes += byteCount

                        val progress = if (totalBytes > 0) {
                            uploadedBytes.toFloat() / totalBytes.toFloat()
                        } else {
                            0f
                        }

                        onProgress(progress)
                    }
                }

                val bufferedSink = progressSink.buffer()
                requestBody.writeTo(bufferedSink)
                bufferedSink.flush()
            }
        }
    }

    /**
     * Cancels any ongoing file transfer operation.
     */
    fun cancelTransfer() {
        isTransferCancelled = true
    }

    /**
     * Cleans up Wi-Fi resources and network callbacks.
     * Should be called when the transfer handler is no longer needed.
     */
    fun cleanup() {
        isTransferCancelled = true
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Network callback might already be unregistered
            }
        }
        networkCallback = null
        connectedNetwork = null
    }
}