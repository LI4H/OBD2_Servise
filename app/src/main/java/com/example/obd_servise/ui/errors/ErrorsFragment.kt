package com.example.obd_servise.ui.errors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.databinding.FragmentErrorsBinding
import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
import com.example.obd_servise.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope

class ErrorsFragment : Fragment() {

    private var _binding: FragmentErrorsBinding? = null
    private val binding get() = _binding!!

    private val errorsViewModel: ErrorsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var errorsAdapter: ErrorsAdapter
    private var obdDeviceConnection: ObdDeviceConnection? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true
            ) {
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

        checkBluetoothPermissions()

        homeViewModel.isDemoActive.observe(viewLifecycleOwner) { isDemoActive ->
            loadErrors(isDemoActive)
        }

        return root
    }

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
            initializeObdDeviceConnection()
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            )
        }
    }

    private fun initializeObdDeviceConnection(): BluetoothSocket? {

        val bluetoothSocket = if (homeViewModel.connectionStatus.value == true) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(requireContext(), "Bluetooth не поддерживается", Toast.LENGTH_SHORT)
                    .show()
                return null
            }

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermission()
                return null
            }

            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                if (device.name.contains(
                        "OBD",
                        ignoreCase = true
                    ) || device.address == "your_device_address"
                ) {
                    return createRfcommSocket(device)
                }
            }

            Toast.makeText(
                requireContext(),
                "Не найдено подходящее OBD устройство",
                Toast.LENGTH_SHORT
            ).show()
            return null
        } else {
            null
            }

        obdDeviceConnection =
            bluetoothSocket?.let { ObdDeviceConnection(it.inputStream(1, 1), it.outputStream()) }

        loadErrors(homeViewModel.isDemoActive.value == true)
        }


    private fun loadErrors(isDemoActive: Boolean) {
        errorsViewModel.loadErrors(
            obdDeviceConnection,
            isDemoActive,
            homeViewModel.connectionStatus.value == true
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createRfcommSocket(device: BluetoothDevice): BluetoothSocket? {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        return try {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Ошибка подключения к устройству", Toast.LENGTH_SHORT).show()
            null
        }
    }

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
