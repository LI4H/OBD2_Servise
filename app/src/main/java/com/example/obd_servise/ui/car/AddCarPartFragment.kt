package com.example.obd_servise.ui.car

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.databinding.FragmentAddCarPartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AddCarPartFragment : Fragment() {

    private var _binding: FragmentAddCarPartBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedCarId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCarPartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        selectedCarId = arguments?.getString("carId")

        // Установка обработчиков для кнопок
        binding.saveButton.setOnClickListener {
            saveCarPart()
        }

        // Находим кнопку отмены по ID и устанавливаем обработчик
        view.findViewById<View>(com.example.obd_servise.R.id.cancelButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveCarPart() {
        val name = binding.nameEditText.text.toString().trim()
        val recommendedMileage =
            binding.recommendedMileageEditText.text.toString().toIntOrNull() ?: 0
        val currentMileage = binding.currentMileageEditText.text.toString().toIntOrNull() ?: 0
        val price = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val notificationsEnabled = binding.notificationsSwitch.isChecked

        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Введите название"
            return
        } else {
            binding.nameInputLayout.error = null
        }

        val condition = calculateCondition(currentMileage, recommendedMileage)
        val addedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Получаем ссылку на все автомобили
        val carsRef = FirebaseDatabase.getInstance().getReference("cars")

        // Ищем автомобиль с isSelected == 1
        carsRef.orderByChild("isSelected").equalTo(1.0).get().addOnSuccessListener { snapshot ->
            val selectedCarSnapshot = snapshot.children.firstOrNull() ?: run {
                Toast.makeText(
                    requireContext(),
                    "Не найден выбранный автомобиль",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            val carId = selectedCarSnapshot.key ?: run {
                Toast.makeText(
                    requireContext(),
                    "Не удалось получить ID автомобиля",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            // Теперь мы знаем carId — можно сохранять деталь
            val database = FirebaseDatabase.getInstance()
            val carPartsRef = database.getReference("cars").child(carId).child("parts")
            val partId = carPartsRef.push().key ?: return@addOnSuccessListener

            val carPart = CarPart(
                id = partId,
                name = name,
                recommendedMileage = recommendedMileage,
                currentMileage = currentMileage,
                condition = condition,
                addedDate = addedDate,
                price = price,
                notificationsEnabled = notificationsEnabled
            )

            val partRef = carPartsRef.child(partId)

            partRef.setValue(carPart)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Комплектующая добавлена", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Log.e("AddCarPartFragment", "Ошибка сохранения", e)
                    Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT)
                        .show()
                }

        }.addOnFailureListener { error ->
            Log.e("AddCarPartFragment", "Ошибка поиска выбранного авто", error)
            Toast.makeText(requireContext(), "Ошибка поиска авто", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateCondition(current: Int, recommended: Int): String {
        if (recommended == 0) return "normal"
        val percentage = (current.toDouble() / recommended) * 100
        return when {
            percentage < 75 -> "normal"
            percentage in 75.0..100.0 -> "warning"
            else -> "critical"
        }
    }

    // Обработчик для кнопки "Отмена"
    fun onCancelClick(view: View) {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}