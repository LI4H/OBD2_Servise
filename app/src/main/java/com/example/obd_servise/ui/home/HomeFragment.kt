package com.example.obd_servise.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import com.example.obd_servise.ui.connection.ConnectionState

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

// Наблюдаем за состоянием подключения к ELM327 через SharedViewModel
        sharedViewModel.elmStatus.observe(viewLifecycleOwner) { elmState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                elmState,
                sharedViewModel.ecuStatus.value
            )
        }

// Наблюдаем за состоянием подключения к ЭБУ через SharedViewModel
        sharedViewModel.ecuStatus.observe(viewLifecycleOwner) { ecuState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                sharedViewModel.elmStatus.value,
                ecuState
            )
        }
        // проблема изменения статуса(иконки
        binding.connectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceSelectionFragment)
        }
        // проблема изменения статуса(иконки
        binding.demoBtn.setOnClickListener {
            homeViewModel.enableDemoMode()
            sharedViewModel.updateElmStatus(ConnectionState.DEMO)
            sharedViewModel.updateEcuStatus(ConnectionState.DEMO)
        }

        binding.exitDemoBtn.setOnClickListener {
            homeViewModel.disableDemoMode()
            sharedViewModel.updateElmStatus(ConnectionState.DISCONNECTED)
            sharedViewModel.updateEcuStatus(ConnectionState.DISCONNECTED)
        }

        binding.exitConnectBtn.setOnClickListener {
            sharedViewModel.updateElmStatus(ConnectionState.DISCONNECTED)
            sharedViewModel.updateEcuStatus(ConnectionState.DISCONNECTED)
            homeViewModel.disableConnect()
        }

    }

    private fun updateUI(isDemo: Boolean?, elmState: ConnectionState?, ecuState: ConnectionState?) {
        when {
            isDemo == true -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.VISIBLE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.demoBtn)
                binding.statusECU.text = getString(R.string.demoBtn)
            }

            elmState == ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.VISIBLE
                binding.statusELM.text = getString(R.string.connected_to_elm)
                binding.statusECU.text = getString(R.string.connected_to_ecu)
            }

            elmState == ConnectionState.CONNECTED && ecuState != ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.connected_to_elm)
                binding.statusECU.text = getString(R.string.connecting_to_ecu)
            }

            elmState != ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.connecting_to_elm)
                binding.statusECU.text = getString(R.string.connected_to_ecu)
            }

            else -> {
                binding.connectBtn.visibility = View.VISIBLE
                binding.demoBtn.visibility = View.VISIBLE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.statusELM)
                binding.statusECU.text = getString(R.string.statusECU)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}