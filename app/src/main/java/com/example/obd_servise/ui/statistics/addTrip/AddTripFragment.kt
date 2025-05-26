package com.example.obd_servise.ui.statistics.addTrip

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.databinding.FragmentAddTripBinding
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AddTripFragment : Fragment() {

    private var _binding: FragmentAddTripBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDatePicker()
        binding.saveTripButton.setOnClickListener { saveTrip() }

        // Принудительно активируем подсказку для всех полей
//        listOf(
//            binding.dateInputLayout,
//            binding.distanceInputLayout,
//            binding.fuelInputLayout,
//            binding.hoursInputLayout
//        ).forEach { layout ->
//            layout.post {
//                // Эти строки гарантируют, что подсказка будет видна всегда
//                layout.isHintAnimationEnabled = false
//                layout.hint = layout.hint // Принудительное обновление
//                layout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
//            }
//        }
    }

    private fun getSelectedCarId(onResult: (String?) -> Unit) {
        val carsRef = FirebaseDatabase.getInstance().getReference("cars")
        carsRef.orderByChild("isSelected").equalTo(1.0).get().addOnSuccessListener { snapshot ->
            val selectedCarSnapshot = snapshot.children.firstOrNull()
            val selectedCarId = selectedCarSnapshot?.key
            onResult(selectedCarId)
        }.addOnFailureListener {
            onResult(null)
        }
    }

    private fun setupDatePicker() {
        binding.dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = format.format(calendar.time)
                    binding.dateEditText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveTrip() {
        val distance = binding.distanceEditText.text.toString().toDoubleOrNull()
        val fuelUsed = binding.fuelEditText.text.toString().toDoubleOrNull()
        val engineHours = binding.hoursEditText.text.toString().toDoubleOrNull()

        if (selectedDate.isEmpty() || distance == null || fuelUsed == null || engineHours == null || engineHours == 0.0) {
            Toast.makeText(
                requireContext(),
                "Пожалуйста, заполните все поля корректно",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        getSelectedCarId { carId ->
            if (carId == null) {
                Toast.makeText(requireContext(), "Не выбран автомобиль", Toast.LENGTH_SHORT).show()
                return@getSelectedCarId
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("cars/$carId/fuelPrice")
            dbRef.get().addOnSuccessListener {
                val fuelPrice = it.getValue(Double::class.java) ?: 0.0

                val avgSpeed = distance / engineHours
                val fuelConsumption = (fuelUsed / distance) * 100
                val fuelCost = fuelUsed * fuelPrice

                // Создаем объект TripEntity
                val newTrip = TripEntity(
                    id = FirebaseDatabase.getInstance().reference.child("trips").push().key ?: "",
                    carId = carId,
                    date = selectedDate,
                    distance = distance,
                    fuelUsed = fuelUsed,
                    engineHours = engineHours,
                    avgSpeed = avgSpeed,
                    fuelConsumption = fuelConsumption,
                    fuelCost = fuelCost,
                    fuelPrice = fuelPrice
                )

                // Сохраняем поездку в Firebase
                val tripRef = FirebaseDatabase.getInstance()
                    .getReference("cars/$carId/trips/${newTrip.id}")

                tripRef.setValue(newTrip).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Поездка сохранена", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}