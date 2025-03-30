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
import com.example.obd_servise.R
import com.example.obd_servise.obd_connection.ui.obd.ObdViewModel

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

    fun enableDemoMode() {
        demoMode.value = 1
        status.value = 0
        _isDemoActive.value = true
        Log.d("HomeViewModel", "Demo Mode Enabled: $_isDemoActive")
    }

    fun disableDemoMode() {
        demoMode.value = 0
        status.value = 0
        _isDemoActive.value = false
        Log.d("HomeViewModel", "Demo Mode Disabled: $_isDemoActive")
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun hasPermissions(checkPermission: (String) -> Int): Boolean {
        return checkPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun requestPermissions(requestPermissions: (Array<String>, Int) -> Unit) {
        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSION_CODE
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToBluetooth(
        obdViewModel: ObdViewModel,
        checkPermission: (String) -> Int,
        showToast: (String) -> Unit
    ) {
        if (checkPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            checkPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            }
        } else {
            showToast(R.string.bluetooth_permission_required.toString())
            return
        }

        val pairedDevices = getBondedDevices(checkPermission, showToast)
        val obdDevice = pairedDevices?.firstOrNull { it.name.contains("OBD", true) }

        obdDevice?.let {
            obdViewModel.connectToDevice(it)
            _connectionStatus.postValue(true)
            showToast(R.string.connecting_to_obd.toString())
        } ?: run {
            _connectionStatus.postValue(false)
            showToast(R.string.obd_device_not_found.toString())
        }
    }

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
