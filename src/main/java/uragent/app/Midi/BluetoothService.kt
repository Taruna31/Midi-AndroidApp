package uragent.app.Midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import uragent.app.Midi.utils.BluetoothHelper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.delay
import uragent.app.Midi.models.MidiFile

class BluetoothService(private val context: Context) {
    private val TAG = "BluetoothService"

    // Standard SerialPortService ID
    private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (!BluetoothHelper.isBluetoothSupported()) {
            Log.e(TAG, "Bluetooth not supported")
            return@withContext false
        }

        if (!BluetoothHelper.hasBluetoothPermissions(context)) {
            Log.e(TAG, "Missing Bluetooth permissions")
            // Consider informing the user or requesting permissions
            return@withContext false
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Re-fetch in case it changed
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth adapter is null or disabled")
            return@withContext false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
            // Cancel discovery as it otherwise slows down the connection.
            bluetoothAdapter!!.cancelDiscovery()
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
            bluetoothSocket?.connect()

            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream

            isConnected = true
            _connectionState.value = true

            // Start listening for incoming data
            startListening()

            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect: ${e.message}")
            close()
            return@withContext false
        }
    }

    private suspend fun startListening() = withContext(Dispatchers.IO) {
        if (inputStream == null) return@withContext

        val buffer = ByteArray(1024)
        var bytes: Int
        val stringBuilder = StringBuilder()

        while (isConnected) {
            try {
                bytes = inputStream!!.read(buffer)
                if (bytes > 0) {
                    val data = String(buffer, 0, bytes)
                    stringBuilder.append(data)

                    // Check if both FILES and DURATION are in the accumulated data
                    if (stringBuilder.contains("FILES:") && stringBuilder.contains("DURATION:")) {
                        _receivedData.value = stringBuilder.toString()
                        stringBuilder.clear() // Reset for next response
                    }

                    Log.d(TAG, "Accumulated: ${stringBuilder}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading: ${e.message}")
                isConnected = false
                _connectionState.value = false
                break
            }
        }
    }


    fun sendCommand(command: String): Boolean {
        if (!isConnected) {
            Log.e(TAG, "Not connected")
            return false
        }

        return try {
            outputStream?.write((command + "\n").toByteArray())
            Log.d(TAG, "Sent: $command")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error sending: ${e.message}")
            isConnected = false
            _connectionState.value = false
            false
        }
    }

    fun playMidi(midiFile: MidiFile): Boolean {
        // Extract index between square brackets
        val regex = """\[(\d+)]""".toRegex()
        val match = regex.find(midiFile.name)

        val index = match?.groupValues?.get(1)  // Just the number inside brackets
        Log.i(TAG, "PLAY:$index:TEMP:${midiFile.temp}\n")
        return if (index != null) {
            sendCommand("PLAY:$index:TEMP:${midiFile.temp}\n")
        } else {
            Log.e(TAG, "Failed to extract index from name: ${midiFile.name}")
            false
        }
    }

    fun stopMidi(): Boolean {
        return sendCommand("STOP")
    }

    fun adjustTemp(midiFile: MidiFile): Boolean {
        Log.i(TAG, "TEMP:${midiFile.temp}\n")
        return sendCommand("TEMP:${midiFile.temp}\n")
    }

    suspend fun getMidiFiles(): List<MidiFile>? {
        if (!isConnected) {
            Log.e(TAG, "Not connected")
            return null
        }

        // Send command to request file list
        if (!sendCommand("LIST")) {
            return null
        }

        var nameList: List<String>? = null
        var durationList: List<String>? = null

        // Wait for response with timeout
        var timeoutCounter = 0
        while (timeoutCounter < 10) {
            val data = _receivedData.value
            Log.d(TAG, "Raw data: $data")

            // Split received data by newline to parse multiple lines
            val parts = data.split("\n")
            for (part in parts) {
                if (part.startsWith("FILES:")) {
                    val filesData = part.removePrefix("FILES:")
                    nameList = filesData.split("|").map { it.trim() }
                    Log.i(TAG, "Received NAMES: $nameList")
                }

                if (part.startsWith("DURATION:")) {
                    val durationData = part.removePrefix("DURATION:")
                    durationList = durationData.split("|").map { it.trim() }
                    Log.i(TAG, "Received DURATIONS: $durationList")
                }
            }

            // If both lists are ready, build the result
            if (nameList != null && durationList != null) {
                val result = nameList.mapIndexed { index, name ->
                    val dur = durationList.getOrNull(index)?.toIntOrNull() ?: 0
                    MidiFile(name = name, duration = dur)
                }
                Log.i("Bluetooth", "Final parsed MIDI list: $result")
                return result
            }

            delay(500)
            timeoutCounter++
        }

        Log.e("Bluetooth", "Timeout waiting for file list")
        return null
    }

    fun close() {
        try {
            isConnected = false
            _connectionState.value = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing: ${e.message}")
        }
    }
}