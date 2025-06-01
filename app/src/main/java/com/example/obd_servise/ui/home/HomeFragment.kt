package com.example.obd_servise.ui.home

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.connection.ConnectionState

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()

    // Флаг для блокировки повторных нажатий
    private var clickLock = false
    private val clickDebouncePeriod = 500L // 0.5 секунды

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        Log.d("Navigation", "HomeFragment created")

        setupClickListeners()
        setupObservers()
        loadSelectedCar()
    }

    private fun setupClickListeners() {
//        binding.carInfoCard.setOnClickListener {
//            if (!clickLock) {
//                clickLock = true
//                Log.d("Navigation", "Car info card clicked, navigating to CarFragment")
//                findNavController().navigate(R.id.action_homeFragment_to_carFragment)
//
//                // Разблокируем через debounce период
//                binding.carInfoCard.postDelayed({
//                    clickLock = false
//                }, clickDebouncePeriod)
//            }
//        }

        binding.connectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceSelectionFragment)
        }

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

    private fun setupObservers() {
        // Наблюдение за статусом подключения ELM327
        sharedViewModel.elmStatus.observe(viewLifecycleOwner) { elmState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                elmState,
                sharedViewModel.ecuStatus.value
            )
        }

        // Наблюдение за статусом подключения ECU
        sharedViewModel.ecuStatus.observe(viewLifecycleOwner) { ecuState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                sharedViewModel.elmStatus.value,
                ecuState
            )
        }
    }

    private fun loadSelectedCar() {
        Log.d("HomeFragment", "Loading selected car")
        carViewModel.getSelectedCar { car ->
            car?.let {
                Log.d("HomeFragment", "Car selected: ${car.name}")
                binding.carNameText.text = it.name
                binding.carBrandText.text = "Brand: ${it.brand}"
                binding.carModelText.text = "Model: ${it.model}"
                binding.carMileageText.text = "Mileage: ${it.mileage} km"
                binding.carFuelTypeText.text = "Fuel: ${it.fuelType}"
                binding.carVinText.text = "VIN: ${it.vin}"

                // Убедимся, что мы не вызываем навигацию здесь
                // Если вам нужно что-то делать при выборе автомобиля, добавьте флаг
                if (homeViewModel.shouldNavigateToCarFragment) {
                    homeViewModel.shouldNavigateToCarFragment = false
                    findNavController().navigate(R.id.action_homeFragment_to_carFragment)
                }
            } ?: run {
                Log.d("HomeFragment", "No car selected, showing placeholders")
                // Если автомобиль не выбран, показываем заглушки
                binding.carNameText.text = "No car selected"
                binding.carBrandText.text = "Brand: --"
                binding.carModelText.text = "Model: --"
                binding.carMileageText.text = "Mileage: -- km"
                binding.carFuelTypeText.text = "Fuel: --"
                binding.carVinText.text = "VIN: --"
            }
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

    override fun onResume() {
        super.onResume()
        Log.d("Navigation", "HomeFragment onResume")
        // Обновляем данные при возвращении на фрагмент
        loadSelectedCar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}