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
//import kotlinx.coroutines.delay

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
    var isPlaying = bluetoothService.isPlaying

    // Current playlist
//    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
//    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    // App background image path
    private val _backgroundImagePath = MutableStateFlow<String?>(null)
    val backgroundImagePath: StateFlow<String?> = _backgroundImagePath.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered MIDI files based on search
    private val _filteredMidiFiles = MutableStateFlow<List<MidiFile>>(emptyList())
    val filteredMidiFiles: StateFlow<List<MidiFile>> = _filteredMidiFiles.asStateFlow()

    // Current tempo
    var tempo = bluetoothService.currentTempo

    // Current progress
    private val _currentProgress = MutableStateFlow<Int?>(null)
//    val currentProgress: StateFlow<Int?> = _currentProgress.asStateFlow()

    // Navigation state
//    private val _showSongList = MutableStateFlow(false)
//    val showSongList: StateFlow<Boolean> = _showSongList.asStateFlow()

    init {
        refresh()
        synchronize()
    }

    @SuppressLint("MissingPermission")
    fun getAvailableDevices() {
        val context = getApplication<Application>().applicationContext
        if (!BluetoothHelper.hasBluetoothPermissions(context)) {
            Log.w(TAG, "getAvailableDevices called without permissions")
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
        stopPlayback()

        if (bluetoothService.playMidi(midiFile)) {
            _currentMidi.value = midiFile
            bluetoothService._isPlaying.value = true
        }
    }

    fun deleteMidi(midiFile: MidiFile) {
        bluetoothService.sendCommand("DELETE:${midiFile.name}")

        // Optimistically update the UI
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

    fun setBackgroundImage(imagePath: String) {
        _backgroundImagePath.value = imagePath
        saveBackgroundImagePath(imagePath)
    }

    private fun savePlaylistsToStorage() {
        Log.d(TAG, "Saving playlists: ${_playlists.value}")
    }

    private fun saveBackgroundImagePath(imagePath: String) {
        Log.d(TAG, "Saving background image path: $imagePath")
    }

    fun startPlayback() {
        viewModelScope.launch {
            currentMidi.value?.let { midi ->
                playMidi(midi)
                // Start progress updates
//                while (isPlaying.value) {
//                    delay(1000)
//                   _currentProgress.value = (_currentProgress.value ?: 0) + 1
//                }
            }
        }
    }

    fun stopPlayback() {
        bluetoothService.stopMidi()
        bluetoothService._isPlaying.value = false
        _currentProgress.value = 0
    }

    fun nextTrack() {
        val currentIndex = midiFiles.value.indexOf(currentMidi.value)
        if (currentIndex < midiFiles.value.size - 1) {
            _currentMidi.value = midiFiles.value[currentIndex + 1]
            _currentProgress.value = 0
            startPlayback()
        }
    }

    fun previousTrack() {
        val currentIndex = midiFiles.value.indexOf(currentMidi.value)
        if (currentIndex > 0) {
            _currentMidi.value = midiFiles.value[currentIndex - 1]
            _currentProgress.value = 0
            startPlayback()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            getAvailableDevices()
            bluetoothService.getMidiFiles()?.let { files ->
                Log.i(TAG, "RECEIVED: $files")
                _midiFiles.value = files
            }
        }
    }

    fun synchronize() {
        viewModelScope.launch {
            bluetoothService.receivedData.collect { data ->
                when (data) {
                    "ISPLAYING" -> {
                        // Update UI to show playing state
                        Log.d(TAG, "Received ISPLAYING")
                    }
                    "STOPPING" -> {
                        // Update UI to show stopped state
                        Log.d(TAG, "Received STOPPING")
                        _currentProgress.value = 0
                    }
                    else -> {
                        // Handle other messages
                        Log.d(TAG, "Received: $data")
                    }
                }
            }
        }
    }

    fun adjustTempo(change: Int) {
        val currentTempo = tempo.value
        val newTempo = (currentTempo + change).coerceIn(1, 9)
        bluetoothService._currentTempo.value = newTempo
        // Update tempo on ESP32
        val tempChange = when {
            change > 0 -> "TEMPUP"
            change < 0 -> "TEMPDWN"
            else -> return
        }
        
        bluetoothService.adjustTemp(tempChange)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.close()
    }
}