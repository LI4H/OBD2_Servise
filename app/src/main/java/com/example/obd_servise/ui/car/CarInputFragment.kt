package com.example.obd_servise.ui.car

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarInputBinding

class CarInputFragment : Fragment() {
    private var carId: String? = null
    private var _binding: FragmentCarInputBinding? = null
    private val binding get() = _binding!!

    private val fuelTypes = listOf("Бензин", "Дизель", "Газ", "Электро")
    private val carViewModel: CarViewModel by activityViewModels()

    private var isEditing = false // флаг редактирования

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        carId = arguments?.getString("carId")

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_selected_item, fuelTypes)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        binding.inputFuelTypeSpinner.adapter = adapter

        // Обработка выбора топлива
        binding.inputFuelTypeSpinner.setSelection(0)
        updateHintsBasedOnFuel(fuelTypes[0])

        binding.inputFuelTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateHintsBasedOnFuel(fuelTypes[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // Если есть carId, значит редактирование
        carId?.takeIf { it.isNotEmpty() }?.let { id ->
            isEditing = true
            carViewModel.loadCarById(id) { carData ->
                binding.inputName.setText(carData.name)
                binding.inputMileage.setText(carData.mileage.toString())
                binding.inputFuelPrice.setText(carData.fuelPrice.toString())
                binding.inputConsumption.setText(carData.consumption.toString())

                val fuelIndex = fuelTypes.indexOf(carData.fuelType)
                if (fuelIndex >= 0) {
                    binding.inputFuelTypeSpinner.setSelection(fuelIndex)
                    updateHintsBasedOnFuel(carData.fuelType)
                }

                // Запоминаем данные во ViewModel для последующего обновления
                carViewModel.name = carData.name
                carViewModel.mileage = carData.mileage
                carViewModel.fuelPrice = carData.fuelPrice
                carViewModel.consumption = carData.consumption
                carViewModel.fuelType = carData.fuelType
                carViewModel.oilFilterKm = carData.oilFilterKm
                carViewModel.cabinFilterKm = carData.cabinFilterKm
                carViewModel.nextServiceKm = carData.nextServiceKm
            }
        }

        binding.buttonNext.setOnClickListener {
            clearErrors()

            val name = binding.inputName.text.toString().trim()
            val mileageText = binding.inputMileage.text.toString().trim()
            val priceText = binding.inputFuelPrice.text.toString().trim()
            val consumptionText = binding.inputConsumption.text.toString().trim()

            var isValid = true

            if (name.isEmpty()) {
                binding.inputName.error = "Введите название авто"
                isValid = false
            }
            if (mileageText.isEmpty()) {
                binding.inputMileage.error = "Введите пробег"
                isValid = false
            }
            if (priceText.isEmpty()) {
                binding.inputFuelPrice.error = "Введите цену"
                isValid = false
            }
            if (consumptionText.isEmpty()) {
                binding.inputConsumption.error = "Введите расход"
                isValid = false
            }

            if (!isValid) {
                Toast.makeText(
                    requireContext(),
                    "Пожалуйста, заполните все поля",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Сохраняем во ViewModel
            carViewModel.name = name
            carViewModel.mileage = mileageText.toInt()
            carViewModel.fuelPrice = priceText.toFloat()
            carViewModel.fuelType = binding.inputFuelTypeSpinner.selectedItem.toString()
            carViewModel.consumption = consumptionText.toFloat()

            // Навигация — во втором фрагменте будет понятно, что это редактирование, если есть carId
            val bundle = Bundle().apply {
                putString("carId", carId ?: "")
            }
            findNavController().navigate(
                R.id.action_carInputFragment_to_carServiceInfoFragment,
                bundle
            )
        }
    }

    private fun clearErrors() {
        binding.inputName.error = null
        binding.inputMileage.error = null
        binding.inputFuelPrice.error = null
        binding.inputConsumption.error = null
    }

    private fun updateHintsBasedOnFuel(fuelType: String) {
        when (fuelType) {
            "Электро" -> {
                binding.inputFuelPrice.hint = "Цена (Р/кВт·ч)"
                binding.inputConsumption.hint = "Расход (кВт·ч/100км)"
            }

            "Газ" -> {
                binding.inputFuelPrice.hint = "Цена газа (Р/м³)"
                binding.inputConsumption.hint = "Расход газа (м³/100км)"
            }

            else -> {
                binding.inputFuelPrice.hint = "Цена топлива (Р/л)"
                binding.inputConsumption.hint = "Расход на 100 км (л)"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
