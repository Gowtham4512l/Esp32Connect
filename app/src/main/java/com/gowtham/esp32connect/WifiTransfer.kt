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
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    // Use background thread for Wi-Fi operations
    private val wifiHandlerThread = HandlerThread("WifiOperations").apply { start() }
    private val wifiHandler = Handler(wifiHandlerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    // HTTP client configured for ESP32 communication
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Enable retry for better reliability
        .connectionPool(okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
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
                        // Process scan results on background thread
                        wifiHandler.post {
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
            }

            val intentFilter =
                android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

            try {
                context.registerReceiver(scanResultsReceiver, intentFilter)

                // Start scan on background thread
                wifiHandler.post {
                    val scanStarted = wifiManager.startScan()
                    if (!scanStarted) {
                        try {
                            context.unregisterReceiver(scanResultsReceiver)
                        } catch (e: Exception) {
                            // Receiver might not be registered
                        }
                        continuation.resume(emptyList())
                    }
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
                // Process connection on background thread
                wifiHandler.post {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+ - Use NetworkRequest API
                            val ssid = scanResult.SSID
                            if (ssid.isNullOrEmpty()) {
                                continuation.resume(false)
                                return@post
                            }

                            val specifier = WifiNetworkSpecifier.Builder()
                                .setSsid(ssid)
                                .setWpa2Passphrase("esp32password") // Default ESP32 password
                                .build()

                            val request = NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                                .setNetworkSpecifier(specifier)
                                .build()

                            networkCallback = object : ConnectivityManager.NetworkCallback() {
                                override fun onAvailable(network: Network) {
                                    connectedNetwork = network
                                    // Add small delay to ensure network is fully established
                                    wifiHandler.postDelayed({
                                        continuation.resume(true)
                                    }, 2000) // 2 second delay
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
                                preSharedKey = "\"esp32password\"" // Default password
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
     * Validates connection to ESP32 device by pinging the default IP.
     */
    private suspend fun validateConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testRequest = Request.Builder()
                .url("http://192.168.4.1/")
                .head()
                .build()

            val call =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectedNetwork != null) {
                    okHttpClient.newBuilder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .socketFactory(connectedNetwork!!.socketFactory)
                        .build()
                        .newCall(testRequest)
                } else {
                    okHttpClient.newCall(testRequest)
                }

            call.execute().use { response ->
                response.isSuccessful || response.code in 400..499
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Transfers a file to the ESP32 device via HTTP POST.
     * Process:
     * 1. Validates connection to ESP32
     * 2. Creates temporary file from URI
     * 3. Sends HTTP POST to ESP32 at 192.168.4.1/upload
     * 4. Reports progress via callback
     * 5. Cleans up temporary files
     *
     * @param fileUri URI of the file to transfer
     * @param onProgress Callback for upload progress (0.0 to 1.0)
     */
    suspend fun transferFile(
        fileUri: Uri,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        isTransferCancelled = false

        // Validate connection first
        if (!validateConnection()) {
            mainHandler.post { onComplete(false) }
            return
        }


        // Perform all file operations on IO dispatcher
        withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                // Create temporary file from URI on IO thread
                val inputStream = context.contentResolver.openInputStream(fileUri)
                tempFile =
                    File(context.cacheDir, "temp_transfer_file_${System.currentTimeMillis()}")

                inputStream?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        // Use buffered copying for better performance
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            if (isTransferCancelled) {
                                throw IOException("Transfer cancelled")
                            }
                        }
                    }
                }

                // Prepare the HTTP request
                val requestBody = tempFile.asRequestBody("image/*".toMediaType())

                val request = Request.Builder()
                    .url("http://192.168.4.1/upload") // Default ESP32 AP IP
                    .post(createProgressRequestBody(requestBody, onProgress))
                    .addHeader("Content-Type", "multipart/form-data")
                    .addHeader("Connection", "keep-alive") // Optimize connection reuse
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

                // Execute HTTP request
                call.execute().use { response ->
                    val success = response.isSuccessful
                    // Report completion on main thread
                    mainHandler.post { onComplete(success) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { onComplete(false) }
            } finally {
                // Clean up temp files on background thread
                tempFile?.let { file ->
                    try {
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }

                // Clean up all temp files
                try {
                    val tempFiles = context.cacheDir.listFiles { file ->
                        file.name.startsWith("temp_transfer_file_")
                    }
                    tempFiles?.forEach {
                        try {
                            it.delete()
                        } catch (e: Exception) {
                            // Ignore individual file cleanup errors
                        }
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
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
                var lastProgressReport = 0L
                val progressUpdateInterval = 100L // Report progress every 100ms max

                val progressSink = object : ForwardingSink(sink) {
                    override fun write(source: okio.Buffer, byteCount: Long) {
                        if (isTransferCancelled) {
                            throw IOException("Transfer cancelled")
                        }

                        super.write(source, byteCount)
                        uploadedBytes += byteCount

                        // Throttle progress updates to prevent UI flooding
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressReport >= progressUpdateInterval) {
                            lastProgressReport = currentTime

                            val progress = if (totalBytes > 0) {
                                (uploadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            // Report progress on main thread efficiently
                            mainHandler.post { onProgress(progress) }
                        }
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

        wifiHandler.post {
            networkCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                } catch (e: Exception) {
                    // Network callback might already be unregistered
                }
            }
            networkCallback = null
            connectedNetwork = null

            // Clean up background thread
            wifiHandlerThread.quitSafely()
        }
    }
}