package com.example.obd_servise.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentStatisticsBinding
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.home.HomeViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var currentFilter = FilterPeriod.LAST_7_DAYS // Фильтр по умолчанию

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var statsChart: LineChart

    private lateinit var statValues: List<TextView>
    private lateinit var statUnits: List<TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        statsChart = binding.statsChart
        setupChart()
        // Настройка фильтров
        setupFilters()
        // Кнопка добавления поездки
        binding.btnAddTrip.setOnClickListener {
            findNavController().navigate(R.id.action_statisticsFragment_to_addTripFragment)
        }

        // Привязка блоков статистики
        bindStatBlocks()

        // В методе onViewCreated замените наблюдение за Room на Firebase:
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                updateUnitsBasedOnFuelType(car.fuelType)
                statisticsViewModel.getTripsForCar(car.id) // id теперь String
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    updateChartWithTrips(trips)
                    updateStatisticsBlocks(trips)
                }
            }
        }


    }

    private fun setupFilters() {
        // Установка обработчиков для RadioButton
        binding.filterGroup.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.radio_all_time -> FilterPeriod.ALL_TIME
                R.id.radio_7_days -> FilterPeriod.LAST_7_DAYS
                R.id.radio_14_days -> FilterPeriod.LAST_14_DAYS
                R.id.radio_30_days -> FilterPeriod.LAST_30_DAYS
                R.id.radio_90_days -> FilterPeriod.LAST_90_DAYS
                R.id.radio_360_days -> FilterPeriod.LAST_360_DAYS
                else -> FilterPeriod.LAST_7_DAYS
            }

            // Обновляем данные при изменении фильтра
            carViewModel.getSelectedCar { selectedCar ->
                selectedCar?.let { car ->
                    statisticsViewModel.getTripsForCar(car.id)
                }
            }
        }

        // Установка фильтра по умолчанию (7 дней)
        binding.radio7Days.isChecked = true
    }

    private fun filterTripsByPeriod(trips: List<TripEntity>): List<TripEntity> {
        if (currentFilter == FilterPeriod.ALL_TIME) return trips

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -currentFilter.days)
        val filterDate = calendar.time

        return trips.filter { trip ->
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val tripDate = dateFormat.parse(trip.date)
                tripDate?.after(filterDate) ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun bindStatBlocks() {
        statValues = listOf(
            binding.root.findViewById(R.id.stat_value1),
            binding.root.findViewById(R.id.stat_value2),
            binding.root.findViewById(R.id.stat_value3),
            binding.root.findViewById(R.id.stat_value4),
            binding.root.findViewById(R.id.stat_value5),
            binding.root.findViewById(R.id.stat_value6),
        )

        statUnits = listOf(
            binding.root.findViewById(R.id.stat_unit1),
            binding.root.findViewById(R.id.stat_unit2),
            binding.root.findViewById(R.id.stat_unit3),
            binding.root.findViewById(R.id.stat_unit4),
            binding.root.findViewById(R.id.stat_unit5),
            binding.root.findViewById(R.id.stat_unit6),
        )
    }

    private fun setupChart() {
        statsChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // Настройка левой оси Y
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 1f
                textColor = Color.BLACK
                textSize = 12f
                axisLineColor = Color.BLACK
                axisLineWidth = 1f
                setDrawAxisLine(true)
                setDrawLabels(true)
            }

            // Настройка правой оси Y (отключена)
            axisRight.isEnabled = false

            // Настройка оси X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.BLACK
                textSize = 10f
                axisLineColor = Color.BLACK
                axisLineWidth = 1f
                setDrawAxisLine(true)
                setDrawLabels(true)
                labelRotationAngle = -45f // Наклон подписей для лучшей читаемости
            }

            legend.isEnabled = false // Отключаем легенду (квадраты с цветами)
            setNoDataText("Нет данных")

            // Включаем отображение значений при касании
            setDrawMarkers(true)
        }
    }

    private fun updateChartWithTrips(trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            statsChart.clear()
            statsChart.invalidate()
            statsChart.setNoDataText("Нет данных за выбранный период")
            return
        }

        // Обратный порядок для отображения (от старых к новым)
        val sortedTrips = trips.sortedBy { it.date }

        // Создание наборов данных
        val createDataSet = { entries: List<Entry>, color: Int, label: String ->
            LineDataSet(entries, label).apply {
                setDrawCircles(true)
                setDrawValues(false)
                lineWidth = 2f
                circleRadius = 4f
                circleHoleRadius = 2f
                setCircleColor(color)
                setColor(color)
                mode = LineDataSet.Mode.CUBIC_BEZIER // Плавные линии
            }
        }

        // Создаем наборы данных для каждого параметра
        val dataSets = listOf(
            createDataSet(
                createEntries(sortedTrips, "fuelConsumption"),
                Color.BLUE,
                "Расход топлива"
            ),
            createDataSet(createEntries(sortedTrips, "avgSpeed"), Color.GREEN, "Средняя скорость"),
            createDataSet(createEntries(sortedTrips, "distance"), Color.RED, "Дистанция"),
            createDataSet(
                createEntries(sortedTrips, "fuelUsed"),
                Color.MAGENTA,
                "Использовано топлива"
            ),
            createDataSet(createEntries(sortedTrips, "fuelCost"), Color.CYAN, "Стоимость топлива"),
            createDataSet(createEntries(sortedTrips, "engineHours"), Color.YELLOW, "Моточасы")
        )

        // Настройка осей
        statsChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(sortedTrips.map { formatDate(it.date) })
            xAxis.labelCount = minOf(sortedTrips.size, 10) // Не более 10 меток

            data = LineData(dataSets)
            notifyDataSetChanged()
            invalidate()
        }
    }

    private fun createEntries(trips: List<TripEntity>, field: String): List<Entry> {
        return trips.mapIndexed { index, trip ->
            val value = when (field) {
                "fuelConsumption" -> trip.fuelConsumption.toFloat()
                "avgSpeed" -> trip.avgSpeed.toFloat()
                "distance" -> trip.distance.toFloat()
                "fuelUsed" -> trip.fuelUsed.toFloat()
                "fuelCost" -> trip.fuelCost.toFloat()
                "engineHours" -> trip.engineHours.toFloat()
                else -> 0f
            }
            Entry(index.toFloat(), value).apply {
                data = when (field) {
                    "fuelConsumption" -> "Расход: ${"%.1f".format(trip.fuelConsumption)} ${statUnits[0].text}"
                    "avgSpeed" -> "Скорость: ${"%.1f".format(trip.avgSpeed)} км/ч"
                    "distance" -> "Дистанция: ${"%.1f".format(trip.distance)} км"
                    "fuelUsed" -> "Топливо: ${"%.1f".format(trip.fuelUsed)} ${statUnits[4].text}"
                    "fuelCost" -> "Стоимость: ${"%.1f".format(trip.fuelCost)} ₽"
                    "engineHours" -> "Моточасы: ${"%.1f".format(trip.engineHours)} ч"
                    else -> ""
                } + "\nДата: ${formatDate(trip.date)}"
            }
        }
    }

    private fun updateStatisticsBlocks(trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            statValues.forEach { it.text = "-" }
            return
        }

        val avgFuel = trips.map { it.fuelConsumption }.average()
        val avgSpeed = trips.map { it.avgSpeed }.average()
        val totalHours = trips.sumOf { it.engineHours }
        val totalDistance = trips.sumOf { it.distance }
        val totalFuel = trips.sumOf { it.fuelUsed }
        val totalCost = trips.sumOf { it.fuelCost }

        val values = listOf(avgFuel, avgSpeed, totalHours, totalDistance, totalFuel, totalCost)
        statValues.zip(values).forEach { (view, value) ->
            view.text = String.format("%.1f", value)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    // Обновленный метод для наблюдения за поездками
    private fun observeTrips() {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                updateUnitsBasedOnFuelType(car.fuelType)
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = filterTripsByPeriod(trips)
                    updateChartWithTrips(filteredTrips)
                    updateStatisticsBlocks(filteredTrips)
                }
            }
        }
    }
    private fun updateUnitsBasedOnFuelType(fuelType: String) {
        when (fuelType) {
            "Электро" -> {
                statUnits[0].text = "кВт·ч/100км"
                statUnits[4].text = "кВт·ч"
            }

            "Газ" -> {
                statUnits[0].text = "м³/100км"
                statUnits[4].text = "м³"
            }

            else -> {
                statUnits[0].text = "л/100км"
                statUnits[4].text = "л"
            }
        }
        statUnits[5].text = "Р"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        enum class FilterPeriod(val days: Int) {
            ALL_TIME(-1),
            LAST_7_DAYS(7),
            LAST_14_DAYS(14),
            LAST_30_DAYS(30),
            LAST_90_DAYS(90),
            LAST_360_DAYS(360)
        }
    }
}
