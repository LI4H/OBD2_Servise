package com.example.obd_servise.ui.statistics

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentTripDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var tripId: String
    private lateinit var carId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = it.getString("tripId") ?: ""
            carId = it.getString("carId") ?: ""
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTripDetailsBinding.bind(view)

        loadTripData()
        setupButtons()
    }

    private fun loadTripData() {
        statisticsViewModel.getTripDetails(carId, tripId).observe(viewLifecycleOwner) { trip ->
            trip?.let {
                binding.apply {
                    tvDate.text = formatDate(it.date)
                    etDistance.setText(it.distance.toString())
//                    etAvgSpeed.setText(it.avgSpeed.toString())
//                    etFuelUsed.setText(it.fuelUsed.toString())
//                    etFuelCost.setText(it.fuelCost.toString())
//                    etEngineHours.setText(it.engineHours.toString())
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveTripChanges()
        }

        binding.btnDelete.setOnClickListener {
            deleteTrip()
        }
    }

    private fun saveTripChanges() {
        val updatedTrip = TripEntity(
            // id = tripId,
            date = binding.tvDate.text.toString(),
            distance = binding.etDistance.text.toString().toDoubleOrNull() ?: 0.0,
//            avgSpeed = binding.etAvgSpeed.text.toString().toDoubleOrNull() ?: 0.0,
//            fuelUsed = binding.etFuelUsed.text.toString().toDoubleOrNull() ?: 0.0,
//            fuelCost = binding.etFuelCost.text.toString().toDoubleOrNull() ?: 0.0,
//            engineHours = binding.etEngineHours.text.toString().toDoubleOrNull() ?: 0.0
        )

//        statisticsViewModel.updateTrip(carId, updatedTrip).observe(viewLifecycleOwner) { success ->
//            if (success) {
//                Toast.makeText(context, "Поездка обновлена", Toast.LENGTH_SHORT).show()
//                findNavController().popBackStack()
//            } else {
//                Toast.makeText(context, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
//            }
//        }
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
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}