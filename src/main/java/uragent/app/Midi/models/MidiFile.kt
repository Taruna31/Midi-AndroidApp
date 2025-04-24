package uragent.app.Midi.models

data class MidiFile(
    val name: String,
    val path: String = "",
    val temp: Int = 1.coerceIn(1, 9),
    val isFavorite: Boolean = false
)