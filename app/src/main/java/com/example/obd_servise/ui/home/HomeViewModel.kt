package com.example.obd_servise.ui.home

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_servise.obd_connection.bluetooth.ObdManager
import com.example.obd_servise.R

class HomeViewModel : ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> get() = _connectionStatus

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
        var status = MutableLiveData(0) // 1 - подключено, 0 - отключено
        var demoMode = MutableLiveData(0) // 1 - демо, 0 - реальный режим
    }

    private val _isDemoActive = MutableLiveData(false)
    val isDemoActive: LiveData<Boolean> get() = _isDemoActive

    private val _isStatusActive = MutableLiveData(false)
    val isStatusActive: LiveData<Boolean> get() = _isStatusActive
    private val obdManager: ObdManager = ObdManager(Application())
    fun enableDemoMode() {
        demoMode.value = 1
        status.value = 0
        _isDemoActive.value = true
        _connectionStatus.value = false
        Log.d("HomeViewModel", "Demo Mode Enabled: $_isDemoActive")
    }

    fun disableDemoMode() {
        demoMode.value = 0
        status.value = 0
        _isDemoActive.value = false
        _connectionStatus.value = false
        Log.d("HomeViewModel", "Demo Mode Disabled: $_isDemoActive")
    }

    //todo самое важное ....
    fun enableConnect() {
        demoMode.value = 0
        status.value = 1
        _isDemoActive.value = false
        _isStatusActive.value = true
        _connectionStatus.value = true
        Log.d("HomeViewModel", "Connect Enabled: $_isStatusActive")
    }

    fun disableConnect() {
        demoMode.value = 0
        status.value = 0
        _isDemoActive.value = false
        _isStatusActive.value = false
        _connectionStatus.value = false
        Log.d("HomeViewModel", "Connect Disabled: $_isDemoActive")
        // Закрываем соединение
        obdManager.disconnect()
    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    fun hasPermissions(checkPermission: (String) -> Int): Boolean {
//        return checkPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
//                checkPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
//                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//    }
//
//    @RequiresApi(Build.VERSION_CODES.S)
//    fun requestPermissions(requestPermissions: (Array<String>, Int) -> Unit) {
//        requestPermissions(
//            arrayOf(
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ),
//            REQUEST_PERMISSION_CODE
//        )
//    }
//    fun checkConnectionStatus() {
//        if (_connectionStatus.value == true) {
//            Log.d("HomeViewModel", "Already connected")
//        } else {
//            Log.d("HomeViewModel", "Not connected, attempting to reconnect")
//            // Здесь можно добавить попытку переподключения
//        }
//    }
//    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//    fun connectToBluetooth(
//
//        checkPermission: (String) -> Int,
//        showToast: (String) -> Unit
//    ) {
//        if (checkPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
//            checkPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
//        ) {
//            if (!bluetoothAdapter.isEnabled) {
//                bluetoothAdapter.enable()
//            }
//        } else {
//            showToast(R.string.bluetooth_permission_required.toString())
//            return
//        }
//
//        val pairedDevices = getBondedDevices(checkPermission, showToast)
//        val obdDevice = pairedDevices?.firstOrNull { it.name.contains("OBD", true) }
//
//        obdDevice?.let {
//
//            _connectionStatus.postValue(true)
//            showToast(R.string.connecting_to_obd.toString())
//        } ?: run {
//            _connectionStatus.postValue(false)
//            showToast(R.string.obd_device_not_found.toString())
//        }
//    }

    private fun getBondedDevices(
        checkPermission: (String) -> Int,
        showToast: (String) -> Unit
    ): Set<BluetoothDevice>? {
        return try {
            if (checkPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.bondedDevices
            } else {
                null
            }
        } catch (e: SecurityException) {
            showToast(R.string.bluetooth_access_error.toString())//, e.message

            null
        }
    }

}
