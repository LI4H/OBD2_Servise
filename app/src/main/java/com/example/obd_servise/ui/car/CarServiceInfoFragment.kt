package com.example.obd_servise.ui.car

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarServiceInfoBinding

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
        carId = arguments?.getString("carId")
        isEditing = !carId.isNullOrEmpty()

        if (isEditing) {
            binding.buttonAddCar.text = "Применить изменения"
            binding.buttonDeleteCar.visibility = View.VISIBLE

            carId?.let { id ->
                carViewModel.loadCarById(id) { carData ->
                    binding.inputOilFilterKm.setText(carData.oilFilterKm.toString())
                    binding.inputCabinFilterKm.setText(carData.cabinFilterKm.toString())
                    binding.inputNextServiceKm.setText(carData.nextServiceKm.toString())

                    // Обновление ViewModel
                    carViewModel.oilFilterKm = carData.oilFilterKm
                    carViewModel.cabinFilterKm = carData.cabinFilterKm
                    carViewModel.nextServiceKm = carData.nextServiceKm
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
                    onFailure = {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка удаления: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

        } else {
            // Новый автомобиль — кнопка только "Добавить"
            binding.buttonDeleteCar.visibility = View.GONE
            binding.buttonAddCar.setText(R.string.buttonAddCar)

            binding.buttonAddCar.setOnClickListener {
                createCar()
            }
        }
    }

    private fun validateInputs(): Boolean {
        clearErrors()

        val oilFilterKmText = binding.inputOilFilterKm.text.toString().trim()
        val cabinFilterKmText = binding.inputCabinFilterKm.text.toString().trim()
        val nextServiceKmText = binding.inputNextServiceKm.text.toString().trim()

        var isValid = true

        if (oilFilterKmText.isEmpty()) {
            binding.inputOilFilterKm.error = "Введите пробег замены масляного фильтра"
            isValid = false
        }

        if (cabinFilterKmText.isEmpty()) {
            binding.inputCabinFilterKm.error = "Введите пробег замены салонного фильтра"
            isValid = false
        }

        if (nextServiceKmText.isEmpty()) {
            binding.inputNextServiceKm.error = "Введите пробег следующего ТО"
            isValid = false
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT)
                .show()
        }

        return isValid
    }

    private fun fillViewModelFromInputs() {
        carViewModel.oilFilterKm = binding.inputOilFilterKm.text.toString().trim().toInt()
        carViewModel.cabinFilterKm = binding.inputCabinFilterKm.text.toString().trim().toInt()
        carViewModel.nextServiceKm = binding.inputNextServiceKm.text.toString().trim().toInt()
    }

    private fun createCar() {
        if (!validateInputs()) return

        fillViewModelFromInputs()

        carViewModel.saveCarToFirebase(
            onSuccess = {
                Toast.makeText(requireContext(), "Авто добавлено", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.nav_car, false)
            },
            onFailure = {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
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
            onFailure = {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun clearErrors() {
        binding.inputOilFilterKm.error = null
        binding.inputCabinFilterKm.error = null
        binding.inputNextServiceKm.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
