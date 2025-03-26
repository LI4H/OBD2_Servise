package com.example.obd_servise.ui.home

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.obd_connection.ui.obd.ObdViewModel

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
        val root = binding.root

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        binding.connectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceSelectionFragment)
        }

        obdViewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            binding.status.text = if (isConnected) "Подключено к OBD" else "Отключено"
            binding.connectBtn.text = if (isConnected) "Отключиться" else "Подключиться"
        }

        if (hasPermissions()) {
            connectToBluetooth()
        } else {
            requestPermissions()
        }

        return root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

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

    private fun getBondedDevices(): Set<BluetoothDevice>? {
        return try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter.bondedDevices
            } else {
                null
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(), "Ошибка доступа к Bluetooth: ${e.message}", Toast.LENGTH_SHORT
            ).show()
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
                connectToBluetooth()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Необходимо предоставить разрешения для работы с Bluetooth",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun connectToBluetooth() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Необходимо предоставить разрешение на доступ к Bluetooth",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = getBondedDevices()
        val obdDevice = pairedDevices?.firstOrNull { it.name.contains("OBD", true) }

        obdDevice?.let {
            obdViewModel.connectToDevice(it)
            Toast.makeText(
                requireContext(), "Подключение к OBD устройству", Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            Toast.makeText(
                requireContext(), "OBD устройство не найдено", Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}