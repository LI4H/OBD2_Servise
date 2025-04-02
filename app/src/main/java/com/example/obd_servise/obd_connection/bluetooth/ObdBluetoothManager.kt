package com.example.obd_servise.obd_connection.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdBluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "ObdBluetoothManager"
        private val OBD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * Возвращает список спаренных Bluetooth устройств.
     */
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Bluetooth permission not granted")
            return null
        }
        return bluetoothAdapter?.bondedDevices
    }

    /**
     * Подключается к выбранному Bluetooth устройству.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }

        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(OBD_UUID)
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            Log.d(TAG, "Connected to OBD-II device: ${device.name}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error connecting to OBD-II device", e)
            closeConnection()
            false
        }
    }

    /**
     * Закрывает Bluetooth-соединение.
     */
    fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
        }
    }

    /**
     * Возвращает поток ввода для чтения данных.
     */
    fun getInputStream(): InputStream? = inputStream

    /**
     * Возвращает поток вывода для отправки данных.
     */
    fun getOutputStream(): OutputStream? = outputStream
}