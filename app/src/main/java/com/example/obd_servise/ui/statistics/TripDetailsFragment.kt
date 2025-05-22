package com.example.obd_servise.ui.statistics

import android.app.DatePickerDialog
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentTripDetailsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
@AndroidEntryPoint
class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!
    private var tripId: String = ""
    private var carId: String = ""
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        tripId = arguments?.getString("tripId").orEmpty()
        carId = arguments?.getString("carId").orEmpty()

        Log.d("TRIP_DETAILS", "Received TripId: $tripId, CarId: $carId")

        // Если tripId пустой, попробуем получить его из ViewModel
        if (tripId.isEmpty()) {
            val trips = statisticsViewModel.trips.value
            if (!trips.isNullOrEmpty()) {
                // Берем первую поездку (или другую логику выбора)
                tripId = trips.first().id
                Log.d("TRIP_DETAILS", "Got tripId from ViewModel: $tripId")
            }
        }

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
                    binding.tvDate.text = formatDate(dateFormat.format(calendar.time))
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

            binding.apply {
                tvDate.text = formatDate(trip.date)
                etDistance.setText(trip.distance.toString())
                etAvgSpeed.setText(trip.avgSpeed.toString())
                etFuelUsed.setText(trip.fuelUsed.toString())
                etFuelCost.setText(trip.fuelCost.toString())
                etEngineHours.setText(trip.engineHours.toString())

                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = dateFormat.parse(trip.date)
                    date?.let { calendar.time = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveTripChanges() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val updatedTrip = TripEntity(
            id = tripId,
            carId = carId,
            date = dateStr,
            distance = binding.etDistance.text.toString().toDoubleOrNull() ?: 0.0,
            avgSpeed = binding.etAvgSpeed.text.toString().toDoubleOrNull() ?: 0.0,
            fuelUsed = binding.etFuelUsed.text.toString().toDoubleOrNull() ?: 0.0,
            fuelCost = binding.etFuelCost.text.toString().toDoubleOrNull() ?: 0.0,
            engineHours = binding.etEngineHours.text.toString().toDoubleOrNull() ?: 0.0,
            fuelConsumption = calculateFuelConsumption()
        )

        statisticsViewModel.updateTrip(updatedTrip).observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Поездка обновлена", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateFuelConsumption(): Double {
        val fuelUsed = binding.etFuelUsed.text.toString().toDoubleOrNull() ?: 0.0
        val distance = binding.etDistance.text.toString().toDoubleOrNull() ?: 1.0
        return (fuelUsed / distance) * 100
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
                statisticsViewModel.deleteTrip(carId, tripId)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            Toast.makeText(context, "Поездка удалена", Toast.LENGTH_SHORT).show()
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

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString // Возвращаем исходную строку, если не удалось распарсить
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