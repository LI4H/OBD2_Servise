package com.example.obd_servise.ui.statistics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.R
import com.example.obd_servise.ui.statistics.TripsAdapter
import com.example.obd_servise.databinding.FragmentAllTripsBinding
import com.example.obd_servise.ui.car.CarViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AllTripsFragment : Fragment(R.layout.fragment_all_trips) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()
    private var _binding: FragmentAllTripsBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedCarId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAllTripsBinding.bind(view)

        setupRecyclerView()
        setupDatePicker()
        observeTrips()


    }

    private fun setupDatePicker() {
        binding.etSearchDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату")
            .setSelection(calendar.timeInMillis)
            .build()
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateStr = dateFormat.format(Date(selectedDate))
            binding.etSearchDate.setText(selectedDateStr)
            filterTripsByDate(selectedDateStr)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun filterTripsByDate(date: String) {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = trips.filter { it.date == date }
                    if (filteredTrips.isEmpty()) {
                        binding.emptyState.text = "Нет поездок за выбранную дату"
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        (binding.recyclerView.adapter as TripsAdapter).updateData(filteredTrips)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = TripsAdapter(emptyList()) { trip ->
                navigateToTripDetails(trip)
            }
        }
    }

    private fun navigateToTripDetails(trip: TripEntity) {
        val bundle = Bundle().apply {
            putString("tripId", trip.id)
            putString("carId", selectedCarId)
        }
        findNavController().navigate(R.id.action_allTripsFragment_to_tripDetailsFragment, bundle)
    }
    private fun observeTrips() {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                selectedCarId = car.id
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    trips?.let { nonNullTrips ->
                        if (nonNullTrips.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            (binding.recyclerView.adapter as? TripsAdapter)?.updateData(nonNullTrips)
                        }
                    }
                }
            } ?: run {
                // Обработка случая, когда selectedCar == null
                binding.emptyState.text = "Автомобиль не выбран"
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}