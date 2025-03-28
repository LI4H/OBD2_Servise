package com.example.obd_servise.ui.errors

import android.Manifest
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.databinding.FragmentErrorsBinding
import com.example.obd_servise.ui.home.HomeViewModel

class ErrorsFragment : Fragment() {

    private var _binding: FragmentErrorsBinding? = null
    private val binding get() = _binding!!

    private val errorsViewModel: ErrorsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var errorsAdapter: ErrorsAdapter

    @RequiresApi(Build.VERSION_CODES.S)
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) @androidx.annotation.RequiresPermission(
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) { permissions ->
            if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true
            ) {
                initializeObdConnection()
            } else {
                Toast.makeText(requireContext(), "Разрешения для Bluetooth не получены", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.S)
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

        if (errorsViewModel.checkBluetoothPermissions(requireContext())) {
            initializeObdConnection()
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }

        homeViewModel.isDemoActive.observe(viewLifecycleOwner) { isDemoActive ->
            errorsViewModel.loadErrors(isDemoActive, homeViewModel.connectionStatus.value == true)
        }

        return root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initializeObdConnection() {
        if (!errorsViewModel.checkBluetoothPermissions(requireContext())) {
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            )
            return
        }

        try {
            val bluetoothSocket: BluetoothSocket? = errorsViewModel.initializeObdDeviceConnection(
                requireContext(), homeViewModel.connectionStatus.value == true
            )
            errorsViewModel.initializeConnection(
                bluetoothSocket,
                homeViewModel.isDemoActive.value == true,
                homeViewModel.connectionStatus.value == true
            )
        } catch (e: SecurityException) {
            Log.e("ErrorsFragment", "Ошибка доступа к Bluetooth", e)
            Toast.makeText(requireContext(), "Ошибка доступа к Bluetooth", Toast.LENGTH_SHORT)
                .show()
        }
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
