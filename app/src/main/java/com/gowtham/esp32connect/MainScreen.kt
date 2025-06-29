package com.gowtham.esp32connect

/**
 * MainScreen.kt contains the primary user interface for ESP32 Connect.
 *
 * This file implements a modern Material 3 design with:
 * - Tabbed interface for Wi-Fi and BLE connections
 * - File selection with validation
 * - Device scanning and connection management
 * - Real-time transfer progress tracking
 * - Comprehensive error handling and user feedback
 *
 * The UI is built using Jetpack Compose with custom theming and animations.
 */

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.gowtham.esp32connect.ui.theme.BluetoothBlue
import com.gowtham.esp32connect.ui.theme.ConnectedGreen
import com.gowtham.esp32connect.ui.theme.ESP32Blue
import com.gowtham.esp32connect.ui.theme.TransferOrange
import com.gowtham.esp32connect.ui.theme.WifiGreen

/**
 * Main application screen that provides the core ESP32 Connect functionality.
 *
 * Features:
 * - File selection and validation
 * - Tabbed interface for Wi-Fi and BLE modes
 * - Device scanning and connection
 * - File transfer with progress tracking
 * - Permission management
 * - Error and status messaging
 *
 * @param modifier Compose modifier for layout customization
 * @param viewModel ViewModel for business logic and state management
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    // === STATE MANAGEMENT ===
    val context = LocalContext.current

    /** Currently selected tab index (0 = Wi-Fi, 1 = BLE) */
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    /** Tracks if permissions have been requested to avoid repeated prompts */
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    /** Tab labels for the connection type selector */
    val tabs = listOf("Wi-Fi", "BLE")

    // === INITIALIZATION ===
    // Initialize ViewModel with Android context on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeWithContext(context)
    }

    // === FILE SELECTION ===
    // File picker launcher with MIME type validation
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Validate file type before selection (only allow supported image formats)
            val mimeType = context.contentResolver.getType(selectedUri)
            if (isValidImageType(mimeType)) {
                viewModel.selectFile(context, selectedUri)
            } else {
                viewModel.showError("Please select a .jpeg or .bmp image file")
            }
        }
    }

    // === PERMISSION MANAGEMENT ===
    // Handle multiple runtime permissions required for Wi-Fi and BLE operations
    val permissionState = rememberMultiplePermissionsState(
        permissions = PermissionHandler.getAllRequiredPermissions()
    )

    // Reset permission request flag when all permissions are granted
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted && hasRequestedPermissions) {
            hasRequestedPermissions = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // === APPLICATION HEADER ===
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = ESP32Blue
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ESP Connect",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Transfer images to ESP32 via Wi-Fi or BLE",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // === CONNECTION TYPE SELECTOR ===
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.6f,
                            animationSpec = tween(300), label = "tab_alpha"
                        )

                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (index == 0) Icons.Default.Wifi else Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (index == 0) WifiGreen else BluetoothBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = animatedAlpha)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // === FILE SELECTION SECTION ===
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Selected File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.selectedFileName.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = viewModel.selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Size: ${viewModel.selectedFileSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No file selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Check permissions before opening file picker
                            if (permissionState.allPermissionsGranted) {
                                filePickerLauncher.launch("image/*")
                            } else {
                                hasRequestedPermissions = true
                                permissionState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Select Image File (.jpeg/.bmp)",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // === CONNECTION MODE CONTENT ===
        item {
            when (selectedTabIndex) {
                0 -> WifiContent(
                    viewModel = viewModel,
                    hasPermissions = permissionState.allPermissionsGranted && PermissionHandler.hasWifiPermissions(
                        context
                    ),
                    onRequestPermissions = {
                        hasRequestedPermissions = true
                        permissionState.launchMultiplePermissionRequest()
                    }
                )

                1 -> BleContent(
                    viewModel = viewModel,
                    hasPermissions = permissionState.allPermissionsGranted && PermissionHandler.hasBluetoothPermissions(
                        context
                    ),
                    onRequestPermissions = {
                        hasRequestedPermissions = true
                        permissionState.launchMultiplePermissionRequest()
                    }
                )
            }
        }

        // === TRANSFER PROGRESS ===
        if (viewModel.isTransferring) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                TransferProgressCard(
                    progress = viewModel.transferProgress,
                    isTransferring = viewModel.isTransferring,
                    onCancel = { viewModel.cancelTransfer() }
                )
            }
        }

        // === STATUS AND ERROR MESSAGING ===
        if (viewModel.errorMessage.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                val isError = viewModel.errorMessage.contains("error", ignoreCase = true) ||
                        viewModel.errorMessage.contains("failed", ignoreCase = true)
                val isSuccess =
                    viewModel.errorMessage.contains("successfully", ignoreCase = true) ||
                            viewModel.errorMessage.contains("Connected to", ignoreCase = true)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSuccess -> MaterialTheme.colorScheme.primaryContainer
                            isError -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isError -> Icons.Default.Error
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when {
                                isSuccess -> ConnectedGreen
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.showError("") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Wi-Fi connection content section.
 *
 * Displays:
 * - Device scanning controls and progress
 * - List of discovered ESP32 Wi-Fi networks
 * - Connection status and transfer options
 * - Permission request prompts if needed
 *
 * @param viewModel ViewModel for Wi-Fi operations
 * @param hasPermissions Whether required permissions are granted
 * @param onRequestPermissions Callback to request missing permissions
 */
@Composable
fun WifiContent(
    viewModel: MainViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = WifiGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ESP32 Devices (Wi-Fi)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (hasPermissions) {
                    IconButton(
                        onClick = { viewModel.scanWifiDevices() },
                        enabled = !viewModel.isScanning
                    ) {
                        if (viewModel.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WifiGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan",
                                tint = WifiGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasPermissions) {
                PermissionRequiredCard(
                    title = "Wi-Fi Permissions Required",
                    description = "Location permissions are required to scan for Wi-Fi networks and connect to ESP32 devices.",
                    onRequestPermissions = onRequestPermissions
                )
            } else if (viewModel.wifiDevices.isEmpty()) {
                EmptyStateCard(
                    title = "No ESP32 devices found",
                    description = "Make sure your ESP32 is in AP mode with SSID containing 'ESP32' or 'ESP'",
                    tips = listOf(
                        "Check ESP32 is powered on",
                        "Verify AP mode is enabled",
                        "Default password: 'esp32pass'"
                    ),
                    icon = Icons.Default.Wifi,
                    iconColor = WifiGreen
                )
            } else {
                Column {
                    viewModel.wifiDevices.forEach { device ->
                        DeviceItem(
                            deviceName = device.SSID ?: "Unknown Network",
                            deviceInfo = "Signal: ${device.level} dBm",
                            deviceType = "Wi-Fi",
                            isConnected = viewModel.connectedDevice == device.SSID,
                            onConnect = { viewModel.connectToWifiDevice(device) },
                            onTransfer = { viewModel.transferFileViaWifi() },
                            canTransfer = viewModel.selectedFileName.isNotEmpty() &&
                                    viewModel.connectedDevice == device.SSID &&
                                    !viewModel.isTransferring
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bluetooth Low Energy connection content section.
 *
 * Displays:
 * - BLE device scanning controls and progress
 * - List of discovered ESP32 BLE devices
 * - Connection status and transfer options
 * - Permission request prompts if needed
 *
 * @param viewModel ViewModel for BLE operations
 * @param hasPermissions Whether required permissions are granted
 * @param onRequestPermissions Callback to request missing permissions
 */
@Composable
@SuppressLint("MissingPermission")
fun BleContent(
    viewModel: MainViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = BluetoothBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ESP32 Devices (BLE)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (hasPermissions) {
                    IconButton(
                        onClick = { viewModel.scanBleDevices() },
                        enabled = !viewModel.isScanning
                    ) {
                        if (viewModel.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BluetoothBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan",
                                tint = BluetoothBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasPermissions) {
                PermissionRequiredCard(
                    title = "Bluetooth Permissions Required",
                    description = "Bluetooth and location permissions are required for BLE scanning and device communication.",
                    onRequestPermissions = onRequestPermissions
                )
            } else if (viewModel.bleDevices.isEmpty()) {
                EmptyStateCard(
                    title = "No ESP32 BLE devices found",
                    description = "Make sure your ESP32 is advertising over Bluetooth Low Energy",
                    tips = listOf(
                        "Check ESP32 BLE is enabled",
                        "Verify service UUID matches",
                        "Device name should contain 'ESP32' or 'ESP'"
                    ),
                    icon = Icons.Default.Bluetooth,
                    iconColor = BluetoothBlue
                )
            } else {
                Column {
                    viewModel.bleDevices.forEach { device ->
                        DeviceItem(
                            deviceName = device.name ?: "Unknown ESP32",
                            deviceInfo = "MAC: ${device.address}",
                            deviceType = "BLE",
                            isConnected = viewModel.connectedDevice == device.address,
                            onConnect = { viewModel.connectToBleDevice(device) },
                            onTransfer = { viewModel.transferFileViaBle() },
                            canTransfer = viewModel.selectedFileName.isNotEmpty() &&
                                    viewModel.connectedDevice == device.address &&
                                    !viewModel.isTransferring
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual device item in the device list.
 *
 * Features:
 * - Animated background color based on connection status
 * - Device type icon (Wi-Fi or Bluetooth)
 * - Connection and transfer action buttons
 * - Visual connection status indicator
 *
 * @param deviceName Display name of the device
 * @param deviceInfo Additional device information (signal, MAC, etc.)
 * @param deviceType "Wi-Fi" or "BLE" for appropriate styling
 * @param isConnected Whether this device is currently connected
 * @param onConnect Callback for connection action
 * @param onTransfer Callback for file transfer action
 * @param canTransfer Whether transfer button should be enabled
 */
@Composable
fun DeviceItem(
    deviceName: String,
    deviceInfo: String,
    deviceType: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onTransfer: () -> Unit,
    canTransfer: Boolean
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "device_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (deviceType == "Wi-Fi") Icons.Default.Wifi else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (deviceType == "Wi-Fi") WifiGreen else BluetoothBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deviceInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConnectedGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isConnected) {
                        Button(
                            onClick = onConnect,
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Connect",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = ConnectedGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onTransfer,
                            enabled = canTransfer,
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TransferOrange,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Transfer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * File transfer progress display card.
 *
 * Shows:
 * - Animated circular and linear progress indicators
 * - Progress percentage
 * - Cancel transfer option
 *
 * @param progress Transfer progress from 0.0 to 1.0
 * @param isTransferring Whether transfer is currently active
 * @param onCancel Callback to cancel the transfer
 */
@Composable
fun TransferProgressCard(
    progress: Float,
    isTransferring: Boolean,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(24.dp),
                        color = TransferOrange,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Transfer Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onCancel) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = TransferOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${(animatedProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Permission request prompt card.
 *
 * Displays when required permissions are not granted, providing:
 * - Clear explanation of why permissions are needed
 * - Action button to request permissions
 * - Security icon for visual context
 *
 * @param title Permission request title
 * @param description Explanation of why permissions are needed
 * @param onRequestPermissions Callback to trigger permission request
 */
@Composable
fun PermissionRequiredCard(
    title: String,
    description: String,
    onRequestPermissions: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Grant Permissions",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Loading state indicator card.
 *
 * Shows a progress spinner with message during scanning operations.
 *
 * @param message Loading message to display
 * @param color Theme color for the progress indicator
 */
@Composable
fun LoadingCard(
    message: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = color,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Empty state display when no devices are found.
 *
 * Provides:
 * - Helpful explanation of why no devices were found
 * - Troubleshooting tips for users
 * - Appropriate icon based on connection type
 *
 * @param title Main empty state message
 * @param description Detailed explanation
 * @param tips List of troubleshooting suggestions
 * @param icon Icon to display (Wi-Fi or Bluetooth)
 * @param iconColor Color for the icon
 */
@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    tips: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            if (tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tips:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        tips.forEach { tip ->
                            Text(
                                text = "• $tip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Validates whether a file's MIME type is supported for transfer.
 *
 * Currently supports:
 * - JPEG images (image/jpeg, image/jpg)
 * - BMP images (image/bmp)
 *
 * @param mimeType MIME type string from ContentResolver
 * @return true if the file type is supported for transfer
 */
private fun isValidImageType(mimeType: String?): Boolean {
    return when (mimeType?.lowercase()) {
        "image/jpeg", "image/jpg", "image/bmp" -> true
        else -> false
    }
}

// ===============================
// COMPOSE PREVIEWS
// ===============================

/**
 * Preview for the main screen showing empty state (no devices found).
 * Displays the Wi-Fi tab with no permissions granted to show the permission request UI.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    EmptyStateCard(
        title = "No ESP32 devices found",
        description = "Make sure your ESP32 is in AP mode with SSID containing 'ESP32' or 'ESP'",
        tips = listOf(
            "Check ESP32 is powered on",
            "Verify AP mode is enabled",
            "Default password: 'esp32pass'"
        ),
        icon = Icons.Default.Wifi,
        iconColor = WifiGreen
    )
}

/**
 * Preview for a connected device item showing transfer capability.
 */
@Preview(showBackground = true)
@Composable
fun DeviceItemConnectedPreview() {
    DeviceItem(
        deviceName = "ESP32_Camera_01",
        deviceInfo = "Signal: -45 dBm",
        deviceType = "Wi-Fi",
        isConnected = true,
        onConnect = { },
        onTransfer = { },
        canTransfer = true
    )
}

/**
 * Preview for a disconnected device item.
 */
@Preview(showBackground = true)
@Composable
fun DeviceItemDisconnectedPreview() {
    DeviceItem(
        deviceName = "ESP32_BLE_Device",
        deviceInfo = "MAC: AA:BB:CC:DD:EE:FF",
        deviceType = "BLE",
        isConnected = false,
        onConnect = { },
        onTransfer = { },
        canTransfer = false
    )
}

/**
 * Preview for the transfer progress card.
 */
@Preview(showBackground = true)
@Composable
fun TransferProgressCardPreview() {
    TransferProgressCard(
        progress = 0.65f,
        isTransferring = true,
        onCancel = { }
    )
}

/**
 * Preview for the permission required card.
 */
@Preview(showBackground = true)
@Composable
fun PermissionRequiredCardPreview() {
    PermissionRequiredCard(
        title = "Wi-Fi Permissions Required",
        description = "Location permissions are required to scan for Wi-Fi networks and connect to ESP32 devices.",
        onRequestPermissions = { }
    )
}

/**
 * Preview for the loading state card.
 */
@Preview(showBackground = true)
@Composable
fun LoadingCardPreview() {
    LoadingCard(
        message = "Scanning for ESP32 Wi-Fi networks...",
        color = WifiGreen
    )
}