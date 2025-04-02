package com.example.obd_servise.obd_connection.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse


class ObdManager(private val context: Context) {
    private var bluetoothManager: ObdBluetoothManager? = null
    private var deviceConnection: DeviceConnection? = null

    /**
     * Инициализирует подключение к OBD устройству.
     */
    fun initialize(): Boolean {
        bluetoothManager = ObdBluetoothManager(context)
        val device = bluetoothManager?.getPairedDevices()?.firstOrNull { it.name.contains("OBD") }
        return if (device != null) {
            // Проверяем разрешения для работы с Bluetooth
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val isConnected = bluetoothManager?.connectToDevice(device) == true
                if (isConnected) {
                    val inputStream = bluetoothManager?.getInputStream()
                    val outputStream = bluetoothManager?.getOutputStream()
                    if (inputStream != null && outputStream != null) {
                        deviceConnection = DeviceConnection(inputStream, outputStream)
                    }
                }
                isConnected
            } else {
                Log.e("ObdManager", "Bluetooth permission not granted")
                false
            }
        } else {
            Log.e("ObdManager", "No paired OBD device found")
            false
        }
    }

    /**
     * Отправляет OBD команду и возвращает результат.
     */
    suspend fun sendCommand(command: ObdCommand, delay: Unit): ObdResponse? {
        return deviceConnection?.run(command) ?: run {
            Log.e("ObdManager", "Device connection is not initialized")
            null
        }
    }

    /**
     * Закрывает соединение с OBD устройством.
     */
    fun disconnect() {
        deviceConnection?.close()
        bluetoothManager?.closeConnection()
    }
}