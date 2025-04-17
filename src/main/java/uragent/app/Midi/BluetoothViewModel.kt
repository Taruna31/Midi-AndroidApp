package uragent.app.Midi

import android.annotation.SuppressLint
import uragent.app.Midi.utils.BluetoothHelper
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uragent.app.Midi.models.MidiFile
import uragent.app.Midi.models.Playlist
import kotlinx.coroutines.flow.update
import android.app.Application
import android.bluetooth.BluetoothManager
import kotlinx.coroutines.delay

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "BluetoothViewModel"

    private val bluetoothService = BluetoothService(application.applicationContext)

    // Bluetooth connection state
    val connectionState = bluetoothService.connectionState

    // List of available Bluetooth devices
    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices.asStateFlow()

    // List of MIDI files on ESP32
    private val _midiFiles = MutableStateFlow<List<MidiFile>>(emptyList())
    val midiFiles: StateFlow<List<MidiFile>> = _midiFiles.asStateFlow()

    // List of playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Currently playing MIDI file
    private val _currentMidi = MutableStateFlow<MidiFile?>(null)
    val currentMidi: StateFlow<MidiFile?> = _currentMidi.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Current playlist
    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    // App background image path
    private val _backgroundImagePath = MutableStateFlow<String?>(null)
    val backgroundImagePath: StateFlow<String?> = _backgroundImagePath.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered MIDI files based on search
    private val _filteredMidiFiles = MutableStateFlow<List<MidiFile>>(emptyList())
    val filteredMidiFiles: StateFlow<List<MidiFile>> = _filteredMidiFiles.asStateFlow()

    private val _tempo = MutableStateFlow<Int?>(null)
    val tempo: StateFlow<Int?> = _tempo.asStateFlow()

    private val _currentProgress = MutableStateFlow<Int?>(null)
    val currentProgress: StateFlow<Int?> = _currentProgress.asStateFlow()

    private val _totalDuration = MutableStateFlow<Int?>(null)
    val totalDuration: StateFlow<Int?> = _totalDuration.asStateFlow()

    // Navigation state
    private val _showSongList = MutableStateFlow(false)
    val showSongList: StateFlow<Boolean> = _showSongList.asStateFlow()

    init {
        // Start collecting received data from BluetoothService
        refresh()
    }

    private fun initializeSampleData() {
        // Sample MIDI files (Paths are illustrative, app relies on ESP32 names)
        val sampleMidiFiles = listOf(
            MidiFile("Rhadu - Abdul Sayangku.mid", duration = 180),
            MidiFile("Rhadu - Dudulku yang maniez.mid", duration = 210),
            MidiFile("Kahlil - Bersama Said Selamanya.mid", duration = 540)
        )

        _midiFiles.value = sampleMidiFiles
        _filteredMidiFiles.value = sampleMidiFiles

        // Sample playlists
        val classicalPlaylist = Playlist(
            name = "Classical Favorites",
            files = sampleMidiFiles
        )

        _playlists.value = listOf(classicalPlaylist)
    }

    @SuppressLint("MissingPermission")
    fun getAvailableDevices() {
        val context = getApplication<Application>().applicationContext
        if (!BluetoothHelper.hasBluetoothPermissions(context)) {
            Log.w(TAG, "getAvailableDevices called without permissions")
            // Maybe trigger a state to inform the UI or request permissions
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null in getAvailableDevices")
            return
        } else {
            val pairedDevices = bluetoothAdapter.bondedDevices
            _availableDevices.value = pairedDevices.toList()
        }

    }

    fun connectToESP32(address: String) {
        viewModelScope.launch {
            val result = bluetoothService.connect(address)
            if (result) {
                // After successful connection, request the MIDI files list
                requestMidiFilesList()
            }
        }
    }

    private fun requestMidiFilesList() {
        bluetoothService.sendCommand("LIST")
    }

    fun searchMidiFiles(query: String) {
        _searchQuery.value = query
        _filteredMidiFiles.value = _midiFiles.value.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    fun playMidi(midiFile: MidiFile) {
        bluetoothService.sendCommand("PLAY:${midiFile.name}")
        _currentMidi.value = midiFile
        _isPlaying.value = true
    }

//    fun stopMidi() {
//        bluetoothService.sendCommand("STOP")
//        _isPlaying.value = false
//    }
//    fun nextMidi() {
//        val currentPlaylist = _currentPlaylist.value ?: return
//        val currentIndex = currentPlaylist.files.indexOfFirst { it.name == _currentMidi.value?.name }
//
//        if (currentIndex >= 0 && currentIndex < currentPlaylist.files.size - 1) {
//            val nextMidi = currentPlaylist.files[currentIndex + 1]
//            playMidi(nextMidi)
//        }
//    }
//    fun previousMidi() {
//        val currentPlaylist = _currentPlaylist.value ?: return
//        val currentIndex = currentPlaylist.files.indexOfFirst { it.name == _currentMidi.value?.name }
//
//        if (currentIndex > 0) {
//            val prevMidi = currentPlaylist.files[currentIndex - 1]
//            playMidi(prevMidi)
//        }
//    }

    fun deleteMidi(midiFile: MidiFile) {
        bluetoothService.sendCommand("DELETE:${midiFile.name}")

        // Optimistically update the UI (will be corrected when ESP32 sends updated list)
        _midiFiles.update { files ->
            files.filter { it.name != midiFile.name }
        }

        _filteredMidiFiles.update { files ->
            files.filter { it.name != midiFile.name }
        }
    }

    fun createPlaylist(name: String, files: List<MidiFile>) {
        val newPlaylist = Playlist(name = name, files = files)
        _playlists.update { playlists ->
            playlists + newPlaylist
        }
        savePlaylistsToStorage()
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.update { playlists ->
            playlists.filter { it.id != playlistId }
        }
        savePlaylistsToStorage()
    }

    fun startPlaylist(playlist: Playlist) {
        _currentPlaylist.value = playlist

        if (playlist.files.isNotEmpty()) {
            playMidi(playlist.files.first())
        }
    }

    fun setBackgroundImage(imagePath: String) {
        _backgroundImagePath.value = imagePath
        saveBackgroundImagePath(imagePath)
    }

    // In a real app, these would save to SharedPreferences or a database
    private fun savePlaylistsToStorage() {
        // Save to SharedPreferences or database
        Log.d(TAG, "Saving playlists: ${_playlists.value}")
    }

    private fun saveBackgroundImagePath(imagePath: String) {
        // Save to SharedPreferences
        Log.d(TAG, "Saving background image path: $imagePath")
    }

    // Process received data from ESP32
    private fun processReceivedData(data: String) {
        if (data.startsWith("FILES:")) {
            // Process list of files
            val filesData = data.removePrefix("FILES:")
            val filesList = filesData.split("|")

            val midiFiles = filesList.map { fileName ->
                MidiFile(name = fileName)
            }

            _midiFiles.value = midiFiles
            _filteredMidiFiles.value = midiFiles}
//          else if (data.startsWith("PLAYING:")) {
//            // Update currently playing file
//            val fileName = data.removePrefix("PLAYING:")
//            _currentMidi.value = _midiFiles.value.find { it.name == fileName }
//            _isPlaying.value = true
//        } else if (data == "STOPPED") {
//            _isPlaying.value = false
//        }
    }

    fun startPlayback() {
        viewModelScope.launch {
            currentMidi.value?.let { midi ->
                _isPlaying.value = true
                bluetoothService.playMidi(midi)
                // Start progress updates
                while (_isPlaying.value == true) {
                    delay(1000)
                    _currentProgress.value = (_currentProgress.value ?: 0) + 1
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        bluetoothService.stopMidi()
        _currentProgress.value = 0
    }

    fun nextTrack() {
        val currentIndex = midiFiles.value.indexOf(currentMidi.value)
        if (currentIndex < midiFiles.value.size - 1) {
            _currentMidi.value = midiFiles.value[currentIndex + 1]
            _totalDuration.value = currentMidi.value?.duration
            _currentProgress.value = 0
            if (_isPlaying.value == true) {
                startPlayback()
            }
        }
    }

    fun previousTrack() {
        val currentIndex = midiFiles.value.indexOf(currentMidi.value)
        if (currentIndex > 0) {
            _currentMidi.value = midiFiles.value[currentIndex - 1]
            _totalDuration.value = currentMidi.value?.duration
            _currentProgress.value = 0
            if (_isPlaying.value == true) {
                startPlayback()
            }
        }
    }

    // One times used
    fun refresh() {
        viewModelScope.launch {
            getAvailableDevices()
            // Refresh MIDI files list from ESP32
            bluetoothService.getMidiFiles()?.let { files ->
                Log.i(TAG,"RECEIVED: $_midiFiles")
                _midiFiles.value = files
            }
        }
    }

    // Continously listen for the change
    fun synchronize() {
        viewModelScope.launch {
            bluetoothService.receivedData.collect { data ->
                // Process received data from ESP32
                processReceivedData(data)
            }
        }
    }

    fun adjustTempo(change: Int) {
        val currentTempo = _tempo.value ?: 1
        val newTempo = (currentTempo + change).coerceIn(1, 9) // Limit speed between x1 and x9
        _tempo.value = newTempo
        viewModelScope.launch {
            _currentMidi.value = _currentMidi.value?.copy(temp = newTempo)
            _currentMidi.value?.let { midi ->
                bluetoothService.adjustTemp(midi)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.close()
    }
}