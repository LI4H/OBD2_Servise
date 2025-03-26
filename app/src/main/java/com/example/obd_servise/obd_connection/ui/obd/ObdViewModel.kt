package com.example.obd_servise.obd_connection.ui.obd

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.obd_servise.obd_connection.bluetooth.ObdBluetoothManager

class ObdViewModel(application: Application) : AndroidViewModel(application) {
  // val isConnected: false
    private val obdManager: ObdBluetoothManager = ObdBluetoothManager(application)
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> get() = _connectionStatus

    // Получение списка подключенных устройств
    fun getPairedDevices(): Set<BluetoothDevice>? {
        return obdManager.getPairedDevices()
    }

    // Подключение к устройству
    fun connectToDevice(device: BluetoothDevice) {
        val isConnected = obdManager.connectToDevice(device)
        _connectionStatus.postValue(isConnected)
    }

    // Отправка команды и получение ответа
    fun sendCommand(command: String): String? {
        return obdManager.sendCommand(command)
    }

    // Отключение от OBD
    fun disconnectFromObd() {
        obdManager.closeConnection()
        _connectionStatus.value = false
    }
}
