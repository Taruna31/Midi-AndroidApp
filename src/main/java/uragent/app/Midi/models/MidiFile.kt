package uragent.app.Midi.models

data class MidiFile(
    val name: String,
    val path: String = "",
    val duration: Int = 0,  // Duration in seconds
    val temp: Int = 5,
    val isFavorite: Boolean = false
)