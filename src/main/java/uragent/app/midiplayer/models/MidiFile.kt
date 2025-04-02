package uragent.app.midiplayer.models

data class MidiFile(
    val name: String,
    val path: String = "",
    val duration: Int = 0,  // Duration in seconds
    val isFavorite: Boolean = false
)