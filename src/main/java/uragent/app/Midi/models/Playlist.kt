package uragent.app.Midi.models

import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val files: List<MidiFile>,
    val dateCreated: Long = System.currentTimeMillis()
)