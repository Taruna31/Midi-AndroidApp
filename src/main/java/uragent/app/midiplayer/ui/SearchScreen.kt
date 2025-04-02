package uragent.app.midiplayer.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uragent.app.midiplayer.BluetoothViewModel
import uragent.app.midiplayer.R
import uragent.app.midiplayer.models.MidiFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BluetoothViewModel,
    onNavigateBack: () -> Unit
) {
    val filteredMidiFiles by viewModel.filteredMidiFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()
    
    var query by remember { mutableStateOf(searchQuery) }
    
    LaunchedEffect(query) {
        viewModel.searchMidiFiles(query)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Connection status
                if (!connectionState) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Not connected to ESP32. Connect first to search for MIDI files.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search MIDI Files") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = connectionState
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Results
                if (filteredMidiFiles.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (query.isEmpty()) 
                                "Enter a search term to find MIDI files" 
                            else 
                                "No MIDI files found matching '$query'",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Search Results (${filteredMidiFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn {
                        items(filteredMidiFiles) { midiFile ->
                            SearchResultItem(
                                midiFile = midiFile,
                                onPlay = { viewModel.playMidi(midiFile) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    midiFile: MidiFile,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                painter = painterResource(id = R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            // File info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = midiFile.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (midiFile.duration > 0) {
                    Text(
                        text = formatDuration(midiFile.duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Play button
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 