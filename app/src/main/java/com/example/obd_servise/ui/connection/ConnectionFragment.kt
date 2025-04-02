package com.example.obd_servise.ui.connection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.obd_servise.R
//import com.example.obd_servise.command.DelayCommand
//import com.example.obd_servise.command.SetDefaultsCommand
import com.example.obd_servise.databinding.FragmentConnectionBinding
import com.example.obd_servise.obd_connection.bluetooth.ObdManager
import com.example.obd_servise.command.SetEchoOffCommand
//import com.example.obd_servise.command.SetLineFeedOffCommand
import com.example.obd_servise.command.SetSpacesOffCommand
import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetAdaptiveTimingCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var statusECU: TextView
    private lateinit var statusELM: TextView
    private lateinit var errorTextView: TextView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var obdManager: ObdManager

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        statusECU = binding.statusECU
        statusELM = binding.statusELM

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        obdManager = ObdManager(requireContext())

        if (hasRequiredPermissions()) {
            checkBluetoothConnection()
        } else {
            requestBluetoothPermissions()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            PERMISSION_REQUEST_CODE
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBluetoothConnection() {
        if (bluetoothAdapter.isEnabled) {
            val device = bluetoothAdapter.bondedDevices.firstOrNull { it.name.contains("OBD") }
            if (device != null) {
                sharedViewModel.updateElmStatus(ConnectionState.CONNECTING)
                connectToObdDevice(device)
            } else {
                sharedViewModel.updateElmStatus(ConnectionState.ERROR)
                sharedViewModel.updateEcuStatus(ConnectionState.ERROR)
//                connectionStatus.text = getString(R.string.obd_device_not_found)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.obd_device_not_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            sharedViewModel.updateElmStatus(ConnectionState.ERROR)
            sharedViewModel.updateEcuStatus(ConnectionState.ERROR)
//            connectionStatus.text = getString(R.string.bluetooth_not_enabled)
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_enabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun connectToObdDevice(device: BluetoothDevice) {
        lifecycleScope.launch {
            val isConnected = obdManager.initialize()
            if (isConnected) {
                sharedViewModel.updateElmStatus(ConnectionState.CONNECTED)
                val isInitialized = initializeElm327(obdManager)
                if (isInitialized) {
                    sharedViewModel.updateEcuStatus(ConnectionState.CONNECTED)
//                    connectionStatus.text = getString(R.string.connection_successful)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.connected_to_obd),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    sharedViewModel.updateEcuStatus(ConnectionState.ERROR)
//                    connectionStatus.text = getString(R.string.devise_connect_error)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.devise_connect_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                sharedViewModel.updateElmStatus(ConnectionState.ERROR)
                sharedViewModel.updateEcuStatus(ConnectionState.ERROR)
//                connectionStatus.text = getString(R.string.connection_error)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.connection_error_obd),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUI(
        elmState: ConnectionState,
        ecuState: ConnectionState,
        errorMessage: String? = null
    ) {
        when (elmState) {
            ConnectionState.CONNECTED -> statusELM.text = getString(R.string.connected_to_elm)
            ConnectionState.CONNECTING -> statusELM.text = getString(R.string.connecting_to_elm)
            ConnectionState.ERROR -> statusELM.text = getString(R.string.error_connecting_to_elm)
            else -> statusELM.text = getString(R.string.statusELM)
        }

        when (ecuState) {
            ConnectionState.CONNECTED -> statusECU.text = getString(R.string.connected_to_ecu)
            ConnectionState.CONNECTING -> statusECU.text = getString(R.string.connecting_to_ecu)
            ConnectionState.ERROR -> statusECU.text = getString(R.string.error_connecting_to_ecu)
            else -> statusECU.text = getString(R.string.statusECU)
        }

        if (!errorMessage.isNullOrEmpty()) {
            errorTextView.text = errorMessage
            errorTextView.visibility = View.VISIBLE
        } else {
            errorTextView.visibility = View.GONE
        }
    }

    private suspend fun initializeElm327(manager: ObdManager): Boolean {
        val initCommands = listOf(

            ResetAdapterCommand(), // ATZ
            //DelayCommand(500), // небольшая задержка после сброса
            //SetDefaultsCommand(), // AT D - сброс настроек адаптера
            SetEchoOffCommand(),   // ATE0
            SelectProtocolCommand(ObdProtocols.AUTO), // ATSP0
            //SetLineFeedOffCommand(),
            SetSpacesOffCommand(), // ATS0
            SetAdaptiveTimingCommand(AdaptiveTimingMode.AUTO_1) // ATAT1
        )
        for (command in initCommands) {
            val response = manager.sendCommand(command, delay(500))
            if (response?.value == null || response.value.contains("ERROR")) {
                return false
            }
        }
        // Проверяем подключение к ЭБУ
        val testResponse1 = manager.sendCommand(SpeedCommand(), delay(500)) // Запрос скорости
        val testResponse2 = manager.sendCommand(RPMCommand(), delay(500))   // Запрос оборотов
        // Если хотя бы одна команда вернула данные, считаем подключение успешным
        return (testResponse1?.value != null && !testResponse1.value.contains("NO DATA")) ||
                (testResponse2?.value != null && !testResponse2.value.contains("NO DATA"))
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}