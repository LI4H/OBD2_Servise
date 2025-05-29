package com.example.obd_servise.ui.statistics.tripDetails

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentTripDetailsBinding
import com.example.obd_servise.ui.car.CarPart
import com.example.obd_servise.ui.statistics.StatisticsViewModel
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!
    private var tripId: String = ""
    private var carId: String = ""
    private var oldDistance: Double = 0.0 // <-- Сохраняем старое расстояние для сравнения
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tripId = arguments?.getString("tripId").orEmpty()
        carId = arguments?.getString("carId").orEmpty()

        Log.d("TRIP_DETAILS", "Received TripId: $tripId, CarId: $carId")

        if (tripId.isEmpty() || carId.isEmpty()) {
            Log.e("TRIP_DETAILS", "Missing tripId or carId")
            showErrorAndGoBack("Ошибка: ID поездки или автомобиля не переданы")
            return
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTripDetailsBinding.bind(view)

        setupDatePicker()
        loadTripData()
        setupButtons()
    }

    private fun setupDatePicker() {
        binding.tvDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    binding.tvDate.setText(formatDate(dateFormat.format(calendar.time)))

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadTripData() {
        statisticsViewModel.getTripDetails(carId, tripId).observe(viewLifecycleOwner) { trip ->
            if (trip == null) {
                showErrorAndGoBack("Поездка не найдена")
                return@observe
            }

            oldDistance = trip.distance // <-- Запоминаем старое значение distance

            binding.apply {
                tvDate.setText(trip.date.toString())
                etDistance.setText(trip.distance.toString())
                etAvgSpeed.setText(trip.avgSpeed.toString())
                etFuelUsed.setText(trip.fuelUsed.toString())
                etFuelCost.setText(trip.fuelCost.toString())
                etEngineHours.setText(trip.engineHours.toString())
            }

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(trip.date)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveTripChanges() {
        val newDistanceStr = binding.etDistance.text.toString()
        val newDistance = newDistanceStr.toDoubleOrNull() ?: return

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val updatedTrip = TripEntity(
            id = tripId,
            carId = carId,
            date = dateStr,
            distance = newDistance,
            avgSpeed = binding.etAvgSpeed.text.toString().toDoubleOrNull() ?: 0.0,
            fuelUsed = binding.etFuelUsed.text.toString().toDoubleOrNull() ?: 0.0,
            fuelCost = binding.etFuelCost.text.toString().toDoubleOrNull() ?: 0.0,
            engineHours = binding.etEngineHours.text.toString().toDoubleOrNull() ?: 0.0,
            fuelConsumption = calculateFuelConsumption(newDistance)
        )

        val delta = (newDistance - oldDistance).toInt()

        statisticsViewModel.updateTrip(updatedTrip).observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Поездка обновлена", Toast.LENGTH_SHORT).show()
                updateCarAndPartsMileage(carId, delta)
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateFuelConsumption(distance: Double): Double {
        val fuelUsed = binding.etFuelUsed.text.toString().toDoubleOrNull() ?: 0.0
        return if (distance > 0) (fuelUsed / distance) * 100 else 0.0
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveTripChanges() }
        binding.btnDelete.setOnClickListener { deleteTrip() }
    }

    private fun deleteTrip() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление поездки")
            .setMessage("Вы уверены, что хотите удалить эту поездку?")
            .setPositiveButton("Удалить") { _, _ ->
                val oldDistanceInt = oldDistance.toInt()

                statisticsViewModel.deleteTrip(carId, tripId)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            Toast.makeText(context, "Поездка удалена", Toast.LENGTH_SHORT).show()
                            updateCarAndPartsMileage(carId, -oldDistanceInt)
                            findNavController().popBackStack()
                        } else {
                            Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateCarAndPartsMileage(carId: String, deltaDistance: Int) {
        val carsRef = FirebaseDatabase.getInstance().getReference("cars")
        val carRef = carsRef.child(carId).child("mileage")

        // ✅ Обновляем пробег авто через Transaction.Handler
        carRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentMileage = mutableData.getValue(Long::class.java) ?: 0L
                mutableData.value = (currentMileage + deltaDistance).coerceAtLeast(0L)
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (committed) {
                    Log.d("TripDetailsFragment", "Пробег авто обновлён на $deltaDistance")
                } else {
                    Log.e("TripDetailsFragment", "Ошибка обновления пробега авто")
                }
            }
        })

        // ✅ Обновляем пробег деталей
        val partsRef = carsRef.child(carId).child("parts")
        partsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (partSnapshot in snapshot.children) {
                    val part = partSnapshot.getValue(CarPart::class.java) ?: continue

                    if (isTripAfterPartAdded(part.addedDate)) {
                        val updatedCurrentMileage = part.currentMileage + deltaDistance
                        partsRef.child(part.id).child("currentMileage")
                            .setValue(updatedCurrentMileage.coerceAtLeast(0))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TripDetailsFragment", "Ошибка обновления деталей: ${error.message}")
            }
        })
    }

    private fun isTripAfterPartAdded(addedDateString: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val addedDate = sdf.parse(addedDateString) ?: return false
        val today = Date()
        return addedDate.before(today)
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Неизвестно"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("TripDetailsFragment", "Ошибка форматирования даты: ${e.message}")
            dateString // Показываем как есть, если ошибка
        }
    }


    private fun showErrorAndGoBack(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}