package com.example.obd_servise.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.obd_connection.ui.obd.ObdViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var obdViewModel: ObdViewModel

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        obdViewModel = ViewModelProvider(requireActivity()).get(ObdViewModel::class.java)

        binding.connectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceSelectionFragment)
        }

        binding.demoBtn.setOnClickListener {
            homeViewModel.enableDemoMode()
        }

        binding.exitDemoBtn.setOnClickListener {
            homeViewModel.disableDemoMode()
        }

        homeViewModel.isDemoActive.observe(viewLifecycleOwner) { isActive ->
            updateUI(isActive, homeViewModel.connectionStatus.value == true)
        }

        homeViewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            updateUI(homeViewModel.isDemoActive.value == true, isConnected)
        }
    }

    private fun updateUI(isDemo: Boolean?, isConnected: Boolean?) {
        if (isDemo == true) {
            binding.connectBtn.visibility = View.GONE
            binding.demoBtn.visibility = View.GONE
            binding.exitDemoBtn.visibility = View.VISIBLE
            binding.status.text = getString(R.string.demoBtn)
        } else {
            binding.connectBtn.visibility = View.VISIBLE
            binding.demoBtn.visibility = View.VISIBLE
            binding.exitDemoBtn.visibility = View.GONE
            binding.status.text =
                if (isConnected == true) getString(R.string.connected_to_obd) else getString(R.string.status_home)
            binding.connectBtn.text =
                if (isConnected == true) getString(R.string.disconnect) else getString(R.string.btn_connection_text)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
