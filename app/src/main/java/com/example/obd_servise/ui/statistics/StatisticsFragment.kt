package com.example.obd_servise.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentStatisticsBinding
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.home.HomeViewModel
import com.example.obd_servise.ui.statistics.TripEntity
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
        binding.btnMyTrips.setOnClickListener {
            carViewModel.getSelectedCar { selectedCar ->
                selectedCar?.let { car ->
                    statisticsViewModel.getTripsForCar(car.id)
                    statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                        trips?.let { nonNullTrips ->
                            if (nonNullTrips.isEmpty()) {
                                Toast.makeText(context, "Поездок еще нет", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                // Проверяем текущий destination
                                val currentDest = findNavController().currentDestination?.id
                                if (currentDest != R.id.allTripsFragment) {
                                    findNavController().navigate(R.id.action_statisticsFragment_to_allTripsFragment)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Привязка блоков статистики
        bindStatBlocks()

        //
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                updateUnitsBasedOnFuelType(car.fuelType)
                statisticsViewModel.getTripsForCar(car.id) // id теперь String
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    val filteredTrips = filterTripsByPeriod(trips)
                    updateChartWithTrips(filteredTrips)
                    updateStatisticsBlocks(filteredTrips)
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
            val radioButtons = listOf(
                binding.radioAllTime,
                binding.radio7Days,
                binding.radio14Days,
                binding.radio30Days,
                binding.radio90Days,
                binding.radio360Days
            )

            radioButtons.forEach { radio ->
                radio.setBackgroundResource(
                    if (radio.isChecked) R.drawable.btn2_selected else R.drawable.btn2_ne_selected
                )
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
                textColor = Color.WHITE
                textSize = 12f
                axisLineColor = Color.WHITE
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
                textColor = Color.WHITE
                textSize = 12f
                axisLineColor = Color.WHITE
                axisLineWidth = 1f
                setDrawAxisLine(true)
                setDrawLabels(true)
                labelRotationAngle = -45f // Наклон подписей для лучшей читаемости

                setAvoidFirstLastClipping(true) // Чтобы первая и последняя подписи не обрезались

                // Добавляем верхнюю ось X для месяцев
                val topXAxis = xAxis
                topXAxis.position = XAxis.XAxisPosition.TOP
                topXAxis.setDrawLabels(true)
                topXAxis.setDrawAxisLine(false)
                topXAxis.setDrawGridLines(false)
                topXAxis.textColor = Color.WHITE

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
        // Фильтруем поездки - удаляем те, где все параметры нулевые
        val filteredTrips = trips.filter { trip ->
            trip.distance > 0 || trip.fuelUsed > 0 || trip.avgSpeed > 0 ||
                    trip.fuelConsumption > 0 || trip.fuelCost > 0 || trip.engineHours > 0
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
                Color.parseColor("#2196F3"), // синий
                "Расход топлива"
            ),
            createDataSet(
                createEntries(sortedTrips, "avgSpeed"),
                Color.parseColor("#FF5722"), // оранжевый
                "Средняя скорость"
            ),
            createDataSet(
                createEntries(sortedTrips, "engineHours"),
                Color.parseColor("#FFEB3B"), // жёлтый
                "Моточасы"
            ),
            createDataSet(
                createEntries(sortedTrips, "distance"),
                Color.parseColor("#E91E63"), // красный
                "Дистанция"
            ),
            createDataSet(
                createEntries(sortedTrips, "fuelUsed"),
                Color.parseColor("#673AB7"), // фиолетовый
                "Использовано топлива"
            ),
            createDataSet(
                createEntries(sortedTrips, "fuelCost"),
                Color.parseColor("#8BC34A"), // зелёный
                "Стоимость топлива"
            )
        )


        // Настройка осей
        statsChart.apply {
            // Форматирование дат для нижней оси X (дни)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val index = value.toInt()
                    return if (index in sortedTrips.indices) {
                        formatDay(sortedTrips[index].date)
                    } else ""
                }
            }

            // Форматирование для верхней оси X (месяцы)
            (xAxis as XAxis).apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        return if (index in sortedTrips.indices) {
                            formatMonth(sortedTrips[index].date)
                        } else ""
                    }
                }
            }

            data = LineData(dataSets)
            notifyDataSetChanged()

            // Автомасштабирование
            if (sortedTrips.size > 10) {
                val scaleX = sortedTrips.size / 10f
                viewPortHandler.setMaximumScaleX(scaleX)
                setVisibleXRangeMaximum(10f)
                moveViewToX(sortedTrips.size.toFloat())
            }

            invalidate()
        }
    }

    //
//        // Настройка осей
//        statsChart.apply {
//            xAxis.valueFormatter = IndexAxisValueFormatter(sortedTrips.map { formatDate(it.date) })
//            xAxis.labelCount = minOf(sortedTrips.size, 10) // Не более 10 меток
//
//            data = LineData(dataSets)
//            notifyDataSetChanged()
//
//
//            // Устанавливаем масштаб графика, если много данных
//            if (sortedTrips.size > 10) {
//                val scaleX = sortedTrips.size / 10f
//                statsChart.viewPortHandler.setMaximumScaleX(scaleX)
//                statsChart.setVisibleXRangeMaximum(10f)
//                statsChart.moveViewToX(sortedTrips.size.toFloat())
//            }
//
//
//            invalidate()
//        }
//    }
    private fun formatDay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            SimpleDateFormat("dd", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatMonth(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            monthFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
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
