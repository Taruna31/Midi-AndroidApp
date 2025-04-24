package uragent.app.Midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import uragent.app.Midi.utils.BluetoothHelper
import java.util.UUID
import kotlinx.coroutines.withTimeout

class BleService(private val context: Context) {
    private val TAG = "BleService"

    // BLE Service UUIDs
    private val MIDI_SERVICE_UUID = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700")
    private val MIDI_CHARACTERISTIC_UUID = UUID.fromString("7772E5DB-3868-4112-A1A9-F2669D106BF3")
    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    private var midiCharacteristic: BluetoothGattCharacteristic? = null
    private var isConnected = false

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTempo = MutableStateFlow(1)
    val currentTempo: StateFlow<Int> = _currentTempo

    private var listResponseDeferred: CompletableDeferred<String>? = null
    private val listMessages = StringBuilder()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    isConnected = true
                    _connectionState.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    isConnected = false
                    _connectionState.value = false
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val midiService = gatt.getService(MIDI_SERVICE_UUID)
                midiService?.let { service ->
                    midiCharacteristic = service.getCharacteristic(MIDI_CHARACTERISTIC_UUID)
                    midiCharacteristic?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = String(value)
            processMessage(message)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (!BluetoothHelper.isBluetoothSupported()) {
            Log.e(TAG, "Bluetooth not supported")
            return@withContext false
        }

        if (!BluetoothHelper.hasBluetoothPermissions(context)) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return@withContext false
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth adapter is null or disabled")
            return@withContext false
        }

        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect: ${e.message}")
            return@withContext false
        }
    }

    private fun processMessage(message: String) {
        Log.d(TAG, "Processing message: $message")
        
        when {
            message == "ISPLAYING" -> {
                _isPlaying.value = true
                _receivedData.value = message
            }
            message == "STOPPING" -> {
                _isPlaying.value = false
                _receivedData.value = message
            }
            message.startsWith("TEMP:") -> {
                val tempo = message.removePrefix("TEMP:").toIntOrNull() ?: 1
                _currentTempo.value = tempo
                _receivedData.value = message
            }
            message == "LIST_BEGIN" -> {
                listMessages.clear()
                listMessages.appendLine(message)
            }
            message == "LIST_END" -> {
                listMessages.appendLine(message)
                listResponseDeferred?.complete(listMessages.toString())
                listResponseDeferred = null
            }
            else -> {
                if (listResponseDeferred != null) {
                    listMessages.appendLine(message)
                }
                _receivedData.value = message
            }
        }
    }

    fun sendCommand(command: String): Boolean {
        if (!isConnected || midiCharacteristic == null) {
            Log.e(TAG, "Not connected or characteristic not found")
            return false
        }

        return try {
            midiCharacteristic?.value = (command + "\n").toByteArray()
            bluetoothGatt?.writeCharacteristic(midiCharacteristic)
            Log.d(TAG, "Sent: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending: ${e.message}")
            false
        }
    }

    fun playMidi(midiFile: MidiFile): Boolean {
        val regex = """\[(\d+)]""".toRegex()
        val match = regex.find(midiFile.name)
        val index = match?.groupValues?.get(1)

        Log.i(TAG, "INDEX:$index\nPLAY")
        return if (index != null) {
            // Sync tempo
            if (midiFile.temp != _currentTempo.value) {
                sendCommand("TEMP:${midiFile.temp}")
            }
            
            // Send index and play commands
            sendCommand("INDEX:$index") && sendCommand("PLAY")
        } else {
            Log.e(TAG, "Failed to extract index from name: ${midiFile.name}")
            false
        }
    }

    fun stopMidi(): Boolean {
        return sendCommand("STOP")
    }

    fun adjustTemp(argument: String): Boolean {
        Log.i(TAG, "TEMP:$argument")
        return sendCommand(argument)
    }

    suspend fun getMidiFiles(): List<MidiFile>? {
        if (!isConnected) {
            Log.e(TAG, "Not connected")
            return null
        }

        val deferred = CompletableDeferred<String>()
        listResponseDeferred = deferred

        if (!sendCommand("LIST")) {
            listResponseDeferred = null
            return null
        }

        val rawData = try {
            withTimeout(6000) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout waiting for LIST_END")
            return null
        }

        val collectedNames = mutableSetOf<String>()
        var tempo = _currentTempo.value
        var totalPages = 0
        val receivedPages = mutableSetOf<Int>()

        // Process the received messages
        val messages = rawData.split("\n").map { it.trim() }
        for (message in messages) {
            Log.d(TAG, "Processing line: $message")
            when {
                message == "LIST_BEGIN" -> Unit
                message.startsWith("LIST_COUNT:") -> {
                    totalPages = message.removePrefix("LIST_COUNT:").trim().toIntOrNull() ?: 0
                    Log.d(TAG, "Total pages: $totalPages")
                }
                message.startsWith("LIST_PAGE_") -> {
                    val pageNumber = message.substringAfter("LIST_PAGE_").substringBefore(":").toIntOrNull()
                    val files = message.substringAfter(":").split(",").map { it.trim() }
                    if (pageNumber != null) {
                        collectedNames.addAll(files)
                        receivedPages.add(pageNumber)
                        Log.d(TAG, "Received page $pageNumber: $files")
                    } else {
                        Log.e(TAG, "Failed to parse page number from: $message")
                    }
                }
                message.startsWith("TEMP:") -> {
                    tempo = message.removePrefix("TEMP:").trim().toIntOrNull() ?: 1
                    Log.d(TAG, "Tempo: $tempo")
                }
                message == "LIST_END" -> Unit
            }
        }

        if (receivedPages.size == totalPages) {
            return collectedNames.map { MidiFile(name = it, temp = tempo) }
        }

        Log.e(TAG, "Incomplete list: received ${receivedPages.size} of $totalPages pages")
        return null
    }

    fun close() {
        try {
            isConnected = false
            _connectionState.value = false
            _isPlaying.value = false
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing: ${e.message}")
        }
    }
} 