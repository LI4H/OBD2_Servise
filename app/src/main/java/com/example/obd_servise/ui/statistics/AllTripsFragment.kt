package com.example.obd_servise.ui.statistics

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentAllTripsBinding
import com.example.obd_servise.ui.car.CarViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AllTripsFragment : Fragment(R.layout.fragment_all_trips) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()
    private var _binding: FragmentAllTripsBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedCarId: String

    private var dateFilterMode = DateFilterMode.MONTH // По умолчанию выбран месяц
    private var selectedMonthYear: String? = null
    private var selectedYear: String? = null
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    enum class DateFilterMode {
        ALL_TIME, SINGLE_DAY, MONTH, YEAR, RANGE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAllTripsBinding.bind(view)

        // Устанавливаем текущий месяц по умолчанию
        selectedMonthYear = monthYearFormat.format(Date())
        binding.etMonthYear.setText(selectedMonthYear)

        setupRecyclerView()
        setupDatePickers()
        setupFilterModeSelector()
        observeTrips()
    }

    private fun setupFilterModeSelector() {
        binding.filterGroup.setOnCheckedChangeListener { _, checkedId ->
            dateFilterMode = when (checkedId) {
                R.id.radio_all -> DateFilterMode.ALL_TIME
                R.id.radio_day -> DateFilterMode.SINGLE_DAY
                R.id.radio_month -> DateFilterMode.MONTH
                R.id.radio_year -> DateFilterMode.YEAR
                R.id.radio_range -> DateFilterMode.RANGE
                else -> DateFilterMode.MONTH
            }
            updateDateInputsVisibility()

            // Применяем фильтр сразу при переключении режима (кроме диапазона)
            when (dateFilterMode) {
                DateFilterMode.ALL_TIME -> showAllTrips()
                DateFilterMode.MONTH -> selectedMonthYear?.let { filterTripsByMonth(it) }
                DateFilterMode.YEAR -> selectedYear?.let { filterTripsByYear(it) }
                else -> {}
            }
        }

//        // Устанавливаем месяц по умолчанию как выбранный
        //    binding.radio_month.isChecked = true
    }

    private fun updateDateInputsVisibility() {
        binding.apply {
            tilSearchDate.visibility =
                if (dateFilterMode == DateFilterMode.SINGLE_DAY) View.VISIBLE else View.GONE
            tilMonthYear.visibility =
                if (dateFilterMode == DateFilterMode.MONTH) View.VISIBLE else View.GONE
            tilYear.visibility =
                if (dateFilterMode == DateFilterMode.YEAR) View.VISIBLE else View.GONE
            rangeDateContainer.visibility =
                if (dateFilterMode == DateFilterMode.RANGE) View.VISIBLE else View.GONE
        }
    }

    private fun setupDatePickers() {
        // Для выбора дня
        binding.etSearchDate.setOnClickListener { showSingleDatePicker() }

        // Для выбора месяца и года
        binding.etMonthYear.setOnClickListener { showMonthYearPicker() }
        binding.etYear.setOnClickListener { showYearPicker() }

        // Для выбора диапазона
        binding.etStartDate.setOnClickListener { showStartDatePicker() }
        binding.etEndDate.setOnClickListener { showEndDatePicker() }
    }

    private fun showSingleDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val selectedDateStr = dateFormat.format(Date(selectedDate))
            binding.etSearchDate.setText(selectedDateStr)
            filterTripsByDate(selectedDateStr)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showMonthYearPicker() {
        // Создаем диалог для выбора года
        val years = (2020..2030).toList()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите год")
            .setItems(years.map { it.toString() }.toTypedArray()) { _, which ->
                val selectedYear = years[which]

                // После выбора года показываем выбор месяца
                val months = arrayOf(
                    "Январь", "Февраль", "Март", "Апрель",
                    "Май", "Июнь", "Июль", "Август",
                    "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
                )

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Выберите месяц")
                    .setItems(months) { _, month ->
                        val monthNumber = month + 1 // Месяцы с 1 до 12
                        val monthYear = "$selectedYear-${monthNumber.toString().padStart(2, '0')}"
                        selectedMonthYear = monthYear
                        binding.etMonthYear.setText(monthYear)
                        filterTripsByMonth(monthYear)
                    }
                    .show()
            }
            .show()
    }

    private fun showYearPicker() {
        val years = (2020..2030).toList()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите год")
            .setItems(years.map { it.toString() }.toTypedArray()) { _, which ->
                val year = years[which].toString()
                selectedYear = year
                binding.etYear.setText(year)
                filterTripsByYear(year)
            }
            .show()
    }

    private fun showStartDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите начальную дату")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            selectedStartDate = dateFormat.format(Date(selectedDate))
            binding.etStartDate.setText(selectedStartDate)
            applyDateFilterIfRangeComplete()
        }

        datePicker.show(parentFragmentManager, "START_DATE_PICKER")
    }

    private fun showEndDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите конечную дату")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            selectedEndDate = dateFormat.format(Date(selectedDate))
            binding.etEndDate.setText(selectedEndDate)
            applyDateFilterIfRangeComplete()
        }

        datePicker.show(parentFragmentManager, "END_DATE_PICKER")
    }

    private fun applyDateFilterIfRangeComplete() {
        if (selectedStartDate != null && selectedEndDate != null) {
            filterTripsByDateRange(selectedStartDate!!, selectedEndDate!!)
        }
    }

    private fun showAllTrips() {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    updateFilteredTripsUI(trips, "Нет поездок")
                }
            }
        }
    }

    private fun filterTripsByDate(date: String) {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = trips?.filter { it.date == date }
                    updateFilteredTripsUI(filteredTrips, "Нет поездок за выбранную дату")
                }
            }
        }
    }

    private fun filterTripsByMonth(monthYear: String) {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = trips?.filter { it.date.startsWith(monthYear) }
                    updateFilteredTripsUI(filteredTrips, "Нет поездок за выбранный месяц")
                }
            }
        }
    }

    private fun filterTripsByYear(year: String) {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = trips?.filter { it.date.startsWith(year) }
                    updateFilteredTripsUI(filteredTrips, "Нет поездок за выбранный год")
                }
            }
        }
    }

    private fun filterTripsByDateRange(startDate: String, endDate: String) {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = trips?.filter { trip ->
                        trip.date in startDate..endDate
                    }
                    updateFilteredTripsUI(filteredTrips, "Нет поездок за выбранный период")
                }
            }
        }
    }

    private fun updateFilteredTripsUI(filteredTrips: List<TripEntity>?, emptyMessage: String) {
        filteredTrips?.let {
            if (it.isEmpty()) {
                binding.emptyState.text = emptyMessage
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                (binding.recyclerView.adapter as? TripsAdapter)?.updateData(it)
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
        if (trip.id.isEmpty()) {
            Log.e("NAVIGATION", "Trip ID is empty, cannot navigate")
            Toast.makeText(requireContext(), "Ошибка: отсутствует ID поездки", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (selectedCarId.isEmpty()) {
            Log.e("NAVIGATION", "Car ID is empty, cannot navigate")
            Toast.makeText(requireContext(), "Ошибка: автомобиль не выбран", Toast.LENGTH_SHORT)
                .show()
            return
        }

        Log.d("NAVIGATION", "Navigating to trip details. TripId: ${trip.id}, CarId: $selectedCarId")
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
                            binding.emptyState.text = "Нет поездок"
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