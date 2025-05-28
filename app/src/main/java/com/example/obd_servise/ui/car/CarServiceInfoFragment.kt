package com.example.obd_servise.ui.car

import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarServiceInfoBinding
import java.util.Calendar

class CarServiceInfoFragment : Fragment() {

    private var _binding: FragmentCarServiceInfoBinding? = null
    private val binding get() = _binding!!

    private val carViewModel: CarViewModel by activityViewModels()

    private var carId: String? = null
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarServiceInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carId = arguments?.getString("carId")
        isEditing = !carId.isNullOrEmpty()

        if (isEditing) {
            binding.buttonAddCar.text = "Применить изменения"
            binding.buttonDeleteCar.visibility = View.VISIBLE

            carId?.let { id ->
                carViewModel.loadCarById(id) { carData ->
                    binding.inputCarBrand.setText(carData.brand)
                    binding.inputCarModel.setText(carData.model)
                    binding.inputYear.setText(carData.year.toString())
                    binding.inputVin.setText(carData.vin)
                    binding.inputLicensePlate.setText(carData.licensePlate)

                    carViewModel.updateCarData(
                        carData.brand,
                        carData.model,
                        carData.year,
                        carData.vin,
                        carData.licensePlate
                    )
                }
            }

            binding.buttonAddCar.setOnClickListener {
                updateCar(carId!!)
            }

            binding.buttonDeleteCar.setOnClickListener {
                carViewModel.deleteCarFromFirebase(
                    carId!!,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Авто удалено", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack(R.id.nav_car, false)
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            requireContext(),
                            "Ошибка удаления: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

        } else {
            binding.buttonDeleteCar.visibility = View.GONE
            binding.buttonAddCar.setText(R.string.buttonAddCar)

            binding.buttonAddCar.setOnClickListener {
                createCar()
            }
        }
    }

    private fun validateInputs(): Boolean {
        clearErrors()
        var isValid = true

        val brand = binding.inputCarBrand.text.toString().trim()
        val model = binding.inputCarModel.text.toString().trim()
        val yearStr = binding.inputYear.text.toString().trim()
        val vin = binding.inputVin.text.toString().trim()

        if (TextUtils.isEmpty(brand)) {
            binding.inputCarBrand.error = "Введите марку автомобиля"
            isValid = false
        }

        if (TextUtils.isEmpty(model)) {
            binding.inputCarModel.error = "Введите модель автомобиля"
            isValid = false
        }

        if (TextUtils.isEmpty(yearStr)) {
            binding.inputYear.error = "Введите год выпуска"
            isValid = false
        } else {
            val year = yearStr.toIntOrNull()
            if (year == null || year < 1900 || year > Calendar.getInstance().get(Calendar.YEAR)) {
                binding.inputYear.error = "Некорректный год"
                isValid = false
            }
        }

        if (TextUtils.isEmpty(vin)) {
            binding.inputVin.error = "Введите VIN-номер"
            isValid = false
        } else if (vin.length != 17) {
            binding.inputVin.error = "VIN должен содержать 17 символов"
            isValid = false
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Пожалуйста, исправьте ошибки", Toast.LENGTH_SHORT)
                .show()
        }

        return isValid
    }

    private fun fillViewModelFromInputs() {
        val brand = binding.inputCarBrand.text.toString().trim()
        val model = binding.inputCarModel.text.toString().trim()
        val year = binding.inputYear.text.toString().trim().toInt()
        val vin = binding.inputVin.text.toString().trim()
        val licensePlate = binding.inputLicensePlate.text.toString().trim()

        carViewModel.updateCarData(brand, model, year, vin, licensePlate)
    }

    private fun createCar() {
        if (!validateInputs()) return

        fillViewModelFromInputs()

        carViewModel.saveCarToFirebase(
            onSuccess = {
                Toast.makeText(requireContext(), "Авто добавлено", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.nav_car, false)
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        )
    }

    private fun updateCar(carId: String) {
        if (!validateInputs()) return

        fillViewModelFromInputs()

        carViewModel.updateCarInFirebase(
            carId,
            onSuccess = {
                Toast.makeText(requireContext(), "Изменения сохранены", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.nav_car, false)
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        )
    }

    private fun clearErrors() {
        binding.inputCarBrand.error = null
        binding.inputCarModel.error = null
        binding.inputYear.error = null
        binding.inputVin.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}