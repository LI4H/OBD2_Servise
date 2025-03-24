package com.example.obd_servise.ui.home

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.obd_connection.ui.obd.ObdViewModel
import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var obdViewModel: ObdViewModel
    private lateinit var bluetoothAdapter: BluetoothAdapter

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        obdViewModel = ViewModelProvider(this).get(ObdViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val textViewStatus: TextView = binding.textHome
        val btnConnect: Button = binding.btnConnect

        obdViewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            textViewStatus.text = if (isConnected) "Подключено к OBD" else "Отключено"
            btnConnect.text = if (isConnected) "Отключиться" else "Подключиться"
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (hasPermissions()) {
            connectToBluetooth() // Если разрешения есть, выполняем подключение
        } else {
            requestPermissions() // Если разрешений нет, запрашиваем их
        }

        val pairedDevices: Set<BluetoothDevice>? = getBondedDevices()
        val obdDevice = pairedDevices?.firstOrNull {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) {
                // Запрашиваем разрешения, если их нет
                requestPermissions()
                return@firstOrNull false
            }
            it.name.contains("OBD", true)
        }

        btnConnect.setOnClickListener {
            obdDevice?.let {
                if (obdViewModel.connectionStatus.value == true) {
                    obdViewModel.disconnectFromObd()
                } else {
                    obdViewModel.connectToDevice(it)
                }
            }
        }

        return binding.root
    }

    // Метод для проверки наличия разрешений
    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Метод для запроса разрешений
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSION_CODE
        )
    }

    // Обновленный метод для получения спаренных устройств с проверкой разрешений
    private fun getBondedDevices(): Set<BluetoothDevice>? {
        return try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.bondedDevices
            } else {
                null
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Ошибка доступа к Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                connectToBluetooth() // Разрешения есть, подключаемся
            } else {
                Toast.makeText(requireContext(), "Необходимо предоставить разрешения для работы с Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToBluetooth() {
        // Проверяем, включен ли Bluetooth
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable() // Включаем Bluetooth
            }
        } else {
            Toast.makeText(requireContext(), "Необходимо предоставить разрешение на доступ к Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем спаренные устройства и ищем OBD-устройство
        val pairedDevices: Set<BluetoothDevice>? = getBondedDevices()
        val obdDevice = pairedDevices?.firstOrNull { it.name.contains("OBD", true) }

        obdDevice?.let {
            obdViewModel.connectToDevice(it) // Подключаемся к OBD устройству через Bluetooth
            Toast.makeText(requireContext(), "Подключение к OBD устройству", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(requireContext(), "OBD устройство не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
