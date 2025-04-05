package uragent.app.midiplayer.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uragent.app.midiplayer.BluetoothViewModel
import uragent.app.midiplayer.models.MidiFile
import uragent.app.midiplayer.models.Playlist
import uragent.app.midiplayer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: BluetoothViewModel
) {
    val playlists by viewModel.playlists.collectAsState()
    val midiFiles by viewModel.midiFiles.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Favorites")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Your Library") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            actions = {
                IconButton(onClick = { /* Add new playlist */ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Playlist")
                }
                IconButton(onClick = { /* Search library */ }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search Library")
                }
            }
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> PlaylistsTab(
                playlists = playlists,
                onPlaylistClick = { playlist -> viewModel.startPlaylist(playlist) }
            )
            1 -> FavoritesTab(
                favorites = midiFiles.filter { it.isFavorite },
                onSongClick = { song -> viewModel.playMidi(song) }
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(playlists) { playlist ->
            PlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist) })
        }
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<MidiFile>,
    onSongClick: (MidiFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(favorites) { song ->
            SongItem(song = song, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist icon
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.queuemusic),
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${playlist.files.size} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SongItem(
    song: MidiFile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song icon
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Duration: ${song.duration}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = { /* Toggle favorite */ }) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
} 