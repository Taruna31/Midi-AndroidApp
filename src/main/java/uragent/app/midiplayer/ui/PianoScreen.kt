package uragent.app.midiplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uragent.app.midiplayer.BluetoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoScreen(
    viewModel: BluetoothViewModel
) {
    val connectionState by viewModel.connectionState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Piano Mode") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        )

        // Piano keys container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Piano keys row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Generate 12 piano keys
                repeat(12) { index ->
                    PianoKey(
                        note = index + 1,
                        enabled = connectionState,
                        onNotePress = { note ->
                            if (connectionState) {
                                viewModel.sendCommand("NOTE:$note")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PianoKey(
    note: Int,
    enabled: Boolean,
    onNotePress: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(180.dp)
            .padding(horizontal = 2.dp)
            .background(
                color = if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .clickable(enabled = enabled) { onNotePress(note) },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = getNoteLabel(note),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

private fun getNoteLabel(note: Int): String {
    return when (note) {
        1 -> "A3"
        2 -> "A#3"
        3 -> "C4"
        4 -> "D4"
        5 -> "E4"
        6 -> "F4"
        7 -> "G4"
        8 -> "A4"
        9 -> "A#4"
        10 -> "B4"
        11 -> "C5"
        12 -> "D5"
        else -> ""
    }
} 