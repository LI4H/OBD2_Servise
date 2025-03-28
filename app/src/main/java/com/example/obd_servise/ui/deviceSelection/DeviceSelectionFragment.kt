package com.example.obd_servise.ui.deviceSelection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentDeviceSelectionBinding
import com.example.obd_servise.obd_connection.bluetooth.ObdBluetoothManager
import com.example.obd_servise.obd_connection.ui.obd.ObdViewModel
import com.example.obd_servise.utils.PermissionHelper

class DeviceSelectionFragment : Fragment() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var obdViewModel: ObdViewModel
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var connectionStatus: TextView
    private lateinit var obdBluetoothManager: ObdBluetoothManager

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDeviceSelectionBinding.inflate(inflater, container, false)

        connectionStatus = binding.root.findViewById(R.id.connectionStatus)

        obdViewModel = ViewModelProvider(requireActivity()).get(ObdViewModel::class.java)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        permissionHelper = PermissionHelper(requireActivity())
        obdBluetoothManager = ObdBluetoothManager(requireContext())

        // Показываем инструкцию по подключению
        val connectionGuide = binding.root.findViewById<TextView>(R.id.connectionGuide)
        connectionGuide.text = "Инструкция по подключению: \n1. Заглушите двигатель\n2. Вставьте разъем OBD\n3. Подключитесь через Bluetooth"

        // Проверяем разрешения через PermissionHelper
        if (permissionHelper.hasBluetoothPermissions()) {
            checkBluetoothConnection()
        } else {
            permissionHelper.requestBluetoothPermissions()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothConnection() {
        // Проверяем разрешение BLUETOOTH_CONNECT
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionHelper.requestBluetoothPermissions()
            return
        }

        // Если Bluetooth выключен, перенаправляем в настройки
        if (!bluetoothAdapter.isEnabled) {
            connectionStatus.text = "Bluetooth не включен. Перейдите в настройки."
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        } else {
            connectionStatus.text = "Статус подключения: Подключаемся..."
            Handler().postDelayed({
                // Попытка подключиться к OBD-устройству
                val pairedDevices = obdBluetoothManager.getPairedDevices()
                if (pairedDevices.isNullOrEmpty()) {
                    connectionStatus.text = "Не найдено сопряжённых устройств."
                    Toast.makeText(requireContext(), "Не найдено OBD-устройств", Toast.LENGTH_SHORT).show()
                } else {
                    val device = pairedDevices.firstOrNull() // Просто берём первое устройство
                    if (device != null) {
                        connectToObdDevice(device)
                    }
                }
            }, 5000)
        }
    }

    private fun connectToObdDevice(device: BluetoothDevice) {
        // Подключение к OBD-устройству
        val isConnected = obdBluetoothManager.connectToDevice(device)

        if (isConnected) {
            connectionStatus.text = "Подключение успешно!"
            Toast.makeText(requireContext(), "Подключено к OBD-устройству", Toast.LENGTH_SHORT).show()

            // Дополнительные действия после подключения, например, отправка команды
            val response = obdBluetoothManager.sendCommand("ATZ") // Пример команды для сброса
            android.util.Log.d("DeviceSelectionFragment", "OBD Response: $response")
        } else {
            connectionStatus.text = "Ошибка подключения"
            Toast.makeText(requireContext(), "Ошибка подключения к OBD-устройству", Toast.LENGTH_SHORT).show()
        }
    }
}
