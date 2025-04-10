package com.example.obd_servise.ui.errors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_servise.R
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ErrorItem(
    val code: String,
    val error_status: String,
    val description: String
)

class ErrorsViewModel : ViewModel() {

    private val _errors = MutableLiveData<List<ErrorItem>>(emptyList())
    val errors: LiveData<List<ErrorItem>> = _errors

    private val allErrors = mutableListOf<ErrorItem>()
    private var obdDeviceConnection: ObdDeviceConnection? = null

    @RequiresApi(Build.VERSION_CODES.S)
    fun checkBluetoothPermissions(context: Context): Boolean {
        val bluetoothConnectPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val bluetoothScanPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        )
        return bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothScanPermission == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun initializeObdDeviceConnection(context: Context, isConnected: Boolean): BluetoothSocket? {
        if (!isConnected) return null

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(
                context,
                context.getString(R.string.bluetooth_not_supported),
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        if (!checkBluetoothPermissions(context)) {
            Toast.makeText(
                context,
                context.getString(R.string.bluetooth_no_permissions),
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        return try {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                if (device.name.contains(
                        "OBD",
                        ignoreCase = true
                    ) || device.address == "your_device_address"
                ) {//todo
                    return createRfcommSocket(device, context)
                }
            }
            Toast.makeText(
                context,
                context.getString(R.string.obd_device_not_found),
                Toast.LENGTH_SHORT
            ).show()
            null
        } catch (e: SecurityException) {
            Log.e("ErrorsViewModel", context.getString(R.string.bluetooth_access_error), e)
            Toast.makeText(
                context,
                context.getString(R.string.bluetooth_access_error),
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createRfcommSocket(device: BluetoothDevice, context: Context): BluetoothSocket? {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")//todo
        return try {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.devise_connect_error),
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }

    fun initializeConnection(
        bluetoothSocket: BluetoothSocket?,
        isDemoActive: Boolean,
        isConnected: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            obdDeviceConnection = bluetoothSocket?.let {
                try {
                    com.github.eltonvs.obd.connection.ObdDeviceConnection(
                        it.inputStream,
                        it.outputStream
                    )
                } catch (e: Exception) {
                    Log.e("ErrorsViewModel", "Ошибка создания ObdDeviceConnection", e)
                    null
                }
            }
            loadErrors(isDemoActive, isConnected)
        }
    }

    fun loadErrors(isDemo: Boolean, isConnected: Boolean) {
        if (!isDemo && !isConnected) {
            updateErrors(
                listOf(
                    ErrorItem(
                        "NO_CONN",
                        "No connection",
                        "OBD is not connected"
                    )
                )
            )
            return
        }



        CoroutineScope(Dispatchers.IO).launch {
            try {
                val errorsList = if (isDemo) generateDemoErrors() else fetchErrorsFromObd()
                withContext(Dispatchers.Main) {
                    updateErrors(errorsList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchErrorsFromObd(): List<ErrorItem> {
        // Реализуйте получение ошибок с устройства OBD
        return emptyList()
    }

    private fun updateErrors(errorsList: List<ErrorItem>) {
        allErrors.clear()
        allErrors.addAll(errorsList)
        _errors.postValue(allErrors)
    }

    fun addError(error: ErrorItem) {
        allErrors.add(error)
        _errors.postValue(allErrors)
    }

    fun removeError(error: ErrorItem) {
        allErrors.remove(error)
        _errors.postValue(allErrors)
    }

    private fun generateDemoErrors(): List<ErrorItem> {
        return listOf(
            ErrorItem(
                "P0301_Demo",
                "Ignition misfire",
                "Misfire detected in cylinder 1"
            ),

            ErrorItem(
                "P0420_Demo",
                "Catalyst efficiency issue",
                "Catalyst efficiency below threshold"
            ),

            ErrorItem(
                "P0171_Demo",
                "Lean fuel mixture",
                "Fuel system too lean (Bank 1)"
            ),

            ErrorItem(
                "P0500_Demo",
                "Speed sensor error",
                "Vehicle speed sensor malfunction"
            ),

            ErrorItem(
                "C1201_Demo",
                "Brake system issue",
                "Error in ABS or ESC system"
            )
        )
    }


    fun clearAllErrors() {
        allErrors.clear()
        _errors.postValue(allErrors)
    }

    fun filterErrors(query: String) {
        _errors.postValue(if (query.isEmpty()) allErrors else allErrors.filter {
            it.code.contains(query, ignoreCase = true) || it.description.contains(
                query,
                ignoreCase = true
            )
        })
    }
}
