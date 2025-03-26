package com.example.obd_servise.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.annotation.RequiresApi
class PermissionHelper(val activity: FragmentActivity) {

    @RequiresApi(Build.VERSION_CODES.S)
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasBluetoothPermissions(): Boolean {
        val permissionsGranted = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        val locationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(activity, locationPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return permissionsGranted && locationPermissionGranted
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                activity,
                bluetoothPermissions + locationPermission,
                REQUEST_PERMISSION_CODE
            )
        } else {
            ActivityCompat.requestPermissions(activity, bluetoothPermissions, REQUEST_PERMISSION_CODE)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(activity, "Разрешения получены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Необходимо предоставить разрешения для работы с Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 1001
    }
}
