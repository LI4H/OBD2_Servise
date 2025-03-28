package com.example.obd_servise.ui.errors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.databinding.FragmentErrorsBinding
import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
import java.util.UUID

class ErrorsFragment : Fragment() {

    private var _binding: FragmentErrorsBinding? = null
    private val binding get() = _binding!!
    private val errorsViewModel: ErrorsViewModel by viewModels()
    private lateinit var errorsAdapter: ErrorsAdapter

    private lateinit var obdDeviceConnection: ObdDeviceConnection

    // Регистрация для запроса разрешений
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true) {
                // Разрешения получены, можно продолжить подключение
                initializeObdDeviceConnection()
            } else {
                Toast.makeText(requireContext(), "Разрешения для Bluetooth не получены", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrorsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupSearch()
        setupClearAllButton()

        // Проверка наличия разрешений
        checkBluetoothPermissions()

        return root
    }

    // Проверка разрешений на использование Bluetooth
    private fun checkBluetoothPermissions() {
        val bluetoothConnectPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val bluetoothScanPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED &&
            bluetoothScanPermission == PackageManager.PERMISSION_GRANTED
        ) {
            // Если разрешения есть, инициализируем соединение
            initializeObdDeviceConnection()
        } else {
            // Запрос разрешений
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            )
        }
    }

    private fun initializeObdDeviceConnection() {
        val bluetoothSocket = getBluetoothSocket(requireContext())
        val inputStream = bluetoothSocket?.inputStream
        val outputStream = bluetoothSocket?.outputStream

        obdDeviceConnection = outputStream?.let {
            if (inputStream != null) {
                ObdDeviceConnection(inputStream, it)
            } else {
                // Если потоки не получены, выводим ошибку
                Toast.makeText(requireContext(), "Ошибка подключения к OBD устройству", Toast.LENGTH_SHORT).show()
                return
            }
        } ?: run {
            Toast.makeText(requireContext(), "Ошибка подключения к OBD устройству", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем ошибки при запуске фрагмента
        errorsViewModel.loadErrors(obdDeviceConnection)
    }

    private fun getBluetoothSocket(context: Context): BluetoothSocket? {
        // Получаем BluetoothAdapter
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show()
            return null
        }

        // Проверка разрешения для Bluetooth Connect
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если разрешение не предоставлено, запрашиваем его
            requestBluetoothPermission()
            return null
        }

        // Ищем все доступные устройства
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

        // Перебираем устройства и ищем подходящее по имени или MAC-адресу
        for (device in pairedDevices) {
            if (device.name.contains("OBD", ignoreCase = true) || device.address == "your_device_address") {
                return createRfcommSocket(device)
            }
        }

        // Если не нашли подходящее устройство, выводим ошибку
        Toast.makeText(context, "Не найдено подходящее OBD устройство", Toast.LENGTH_SHORT).show()
        return null
    }

    private fun createRfcommSocket(device: BluetoothDevice): BluetoothSocket? {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        // Проверка разрешения для Bluetooth Connect
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Если разрешение не предоставлено, запрашиваем его
            requestBluetoothPermission()
            return null
        }

        return try {
            // Создаем соединение с устройством по UUID
            device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Ошибка подключения к устройству", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Метод для запроса разрешений
    private fun requestBluetoothPermission() {
        requestPermissionsLauncher.launch(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        )
    }


    private fun setupRecyclerView() {
        errorsAdapter = ErrorsAdapter(mutableListOf())
        binding.errorsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = errorsAdapter
        }

        // Наблюдаем за ошибками в ViewModel
        errorsViewModel.errors.observe(viewLifecycleOwner) { errors ->
            errorsAdapter.updateErrors(errors)
        }
    }

    private fun setupSearch() {
        binding.searchErrors.addTextChangedListener { text ->
            errorsViewModel.filterErrors(text.toString())
        }
    }

    private fun setupClearAllButton() {
        binding.resetAllBtn.setOnClickListener {
            errorsViewModel.clearAllErrors()
            Toast.makeText(requireContext(), "Все ошибки сброшены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
