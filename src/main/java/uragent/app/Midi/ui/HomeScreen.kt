package uragent.app.Midi.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uragent.app.Midi.BluetoothViewModel
import uragent.app.Midi.R
import uragent.app.Midi.utils.BluetoothHelper
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import coil.compose.AsyncImage
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BluetoothViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val devices by viewModel.availableDevices.collectAsState()
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showBluetoothDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!connectionState) {
            viewModel.getAvailableDevices()
            showBluetoothDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MIDI Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showBluetoothDialog = true }) {
                        Icon(
                            painter = if (connectionState) painterResource(id = R.drawable.bluetooth_on)
                                          else painterResource(id = R.drawable.bluetooth_off),
                            contentDescription = "Bluetooth"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
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
            
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status message
                if (connectionState) {
                    Text(
                        text = stringResource(R.string.connection_status_connected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.connection_status_disconnected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Menu options
                MenuOption(
                    title = "Play Music",
                    icon = Icons.Default.PlayArrow,
                    onClick = onNavigateToPlayer,
                    enabled = connectionState
                )
                
                MenuOption(
                    title = "Playlists",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onNavigateToPlaylist
                )
                
                MenuOption(
                    title = "Search",
                    icon = Icons.Default.Search,
                    onClick = onNavigateToSearch,
                    enabled = connectionState
                )
                
                MenuOption(
                    title = "Favorites",
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateToFavorites
                )
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
                    Text("No paired devices found")
                } else {
                    LazyColumn {
                        items(devices) { device ->
                            BluetoothDeviceItem(device, context= LocalContext.current) {
                                scope.launch {
                                    viewModel.connectToESP32(device.address)
                                    showBluetoothDialog = false
                                }
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
fun MenuOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun BluetoothDeviceItem(device: BluetoothDevice, context: Context, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
} 