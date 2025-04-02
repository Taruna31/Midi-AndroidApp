package uragent.app.midiplayer.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uragent.app.midiplayer.BluetoothViewModel
import uragent.app.midiplayer.R
import uragent.app.midiplayer.utils.BluetoothHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BluetoothViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Get a persistent URI
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.setBackgroundImage(uri.toString())
            }
        }
    }

    // Bluetooth devices launcher
    var showBluetoothDialog by remember { mutableStateOf(false) }
    val devices by viewModel.availableDevices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            backgroundImagePath?.let { path ->
                AsyncImage(
                    model = Uri.parse(path),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = "App Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Background image setting
                    SettingItem(
                        title = "Change Background Image",
                        icon = Icons.Default.AccountBox,
                        description = backgroundImagePath?.let { "Image selected" } ?: "No image selected",
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*"
                            }
                            imagePickerLauncher.launch(intent)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Bluetooth setting
                    SettingItem(
                        title = "Bluetooth Connection",
                        icon = Icons.Default.Build,
                        description = if (connectionState) "Connected" else "Disconnected",
                        onClick = {
                            viewModel.getAvailableDevices()
                            showBluetoothDialog = true
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // App info
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    SettingItem(
                        title = "MIDI Player",
                        icon = Icons.Default.Info,
                        description = "Version 1.0",
                        onClick = { }
                    )
                }
            }
        }
    }

    // Bluetooth devices dialog
    if (showBluetoothDialog) {
        AlertDialog(
            onDismissRequest = { showBluetoothDialog = false },
            title = { Text("Select Bluetooth Device") },
            text = {
                if (devices.isEmpty()) {
                    Column {
                        Text("No paired devices found")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please pair with your ESP32 device in system settings first.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn {
                        items(devices.size) { index ->
                            val device = devices[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.connectToESP32(device.address)
                                        showBluetoothDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.bluetooth_off),
                                    contentDescription = "Bluetooth Device",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = BluetoothHelper.getDisplayName(context, device),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (index < devices.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBluetoothDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 