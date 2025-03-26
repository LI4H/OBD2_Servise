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
import androidx.core.content.ContextCompat
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

    // Получение подключенных устройств
    fun getPairedDevices(): Set<BluetoothDevice>? {
        // Проверка разрешения для Bluetooth
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если разрешение не предоставлено, можно запросить его у пользователя
            Log.e(TAG, "Bluetooth permission not granted")
            return null
        }

        return bluetoothAdapter?.bondedDevices
    }

    // Подключение к OBD-устройству
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice): Boolean {
        // Проверка разрешений
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }

        // Проверяем разрешения на использование Bluetooth
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth permissions are missing")
            return false
        }

        // Попытка подключиться к устройству
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

    // Закрытие соединения
    fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        }
    }

    // Отправка команды и получение ответа
    fun sendCommand(command: String): String? {
        return if (outputStream != null && inputStream != null) {
            try {
                outputStream?.write((command + "\r").toByteArray())
                outputStream?.flush()
                Thread.sleep(200) // Задержка для ответа
                val buffer = ByteArray(1024)
                val bytesRead = inputStream?.read(buffer) ?: 0
                String(buffer, 0, bytesRead)
            } catch (e: IOException) {
                Log.e(TAG, "Error sending command", e)
                null
            }
        } else {
            Log.e(TAG, "Streams not initialized")
            null
        }
    }

    // Геттеры для InputStream и OutputStream
    fun getInputStream(): InputStream? = inputStream
    fun getOutputStream(): OutputStream? = outputStream
}
