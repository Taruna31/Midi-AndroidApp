package uragent.app.Midi.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import uragent.app.Midi.BluetoothViewModel
import uragent.app.Midi.R
import uragent.app.Midi.models.MidiFile
import uragent.app.Midi.models.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: BluetoothViewModel,
    onNavigateBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val midiFiles by viewModel.midiFiles.collectAsState()
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showPlaylistSongsDialog by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
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
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (playlists.isEmpty()) {
                    EmptyPlaylistView(onCreateClick = { showCreatePlaylistDialog = true })
                } else {
                    Text(
                        text = "Your Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { showPlaylistSongsDialog = playlist },
                                onDelete = { showDeleteDialog = playlist }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            availableMidiFiles = midiFiles,
            onDismiss = { showCreatePlaylistDialog = false },
            onCreatePlaylist = { name, selectedFiles ->
                viewModel.createPlaylist(name, selectedFiles)
                showCreatePlaylistDialog = false
            }
        )
    }

    // Show playlist songs dialog with play buttons
    showPlaylistSongsDialog?.let { playlist ->
        PlaylistSongsDialog(
            playlist = playlist,
            onDismiss = { showPlaylistSongsDialog = null },
            onPlaySong = { midiFile ->
                viewModel.playMidi(midiFile)
                showPlaylistSongsDialog = null
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { playlist ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete playlist '${playlist.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun EmptyPlaylistView(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Belum ada playlist",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onCreateClick) {
                Text("Buat Playlist")
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.queuemusic),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${playlist.files.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    availableMidiFiles: List<MidiFile>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String, List<MidiFile>) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    val selectedFiles = remember { mutableStateListOf<MidiFile>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Buat Playlist",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nama Playlist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Plihan Lagu: ",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (availableMidiFiles.isEmpty()) {
                    Text(
                        text = "No MIDI files available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(availableMidiFiles) { midiFile ->
                            MidiFileItem(
                                midiFile = midiFile,
                                isSelected = selectedFiles.contains(midiFile),
                                onToggleSelection = { isSelected ->
                                    if (isSelected) {
                                        selectedFiles.add(midiFile)
                                    } else {
                                        selectedFiles.remove(midiFile)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank() && selectedFiles.isNotEmpty()) {
                                onCreatePlaylist(playlistName, selectedFiles.toList())
                            }
                        },
                        enabled = playlistName.isNotBlank() && selectedFiles.isNotEmpty()
                    ) {
                        Text("Buat")
                    }
                }
            }
        }
    }
}

@Composable
fun MidiFileItem(
    midiFile: MidiFile,
    isSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(!isSelected) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = midiFile.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PlaylistSongsDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onPlaySong: (MidiFile) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Lagu dari '${playlist.name}'",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (playlist.files.isEmpty()) {
                    Text(
                        text = "No songs in this playlist",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        itemsIndexed(playlist.files) { index, midiFile ->
                            SongItem(
                                index = index + 1,
                                midiFile = midiFile,
                                onPlay = { onPlaySong(midiFile) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun SongItem(
    index: Int,
    midiFile: MidiFile,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(28.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = midiFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}