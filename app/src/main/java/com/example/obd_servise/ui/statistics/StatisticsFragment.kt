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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

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

        // Кнопка добавления поездки
        binding.btnAddTrip.setOnClickListener {
            findNavController().navigate(R.id.action_statisticsFragment_to_addTripFragment)
        }

        // Привязка блоков статистики
        bindStatBlocks()

        // Получаем выбранную машину и наблюдаем за поездками
//        carViewModel.getSelectedCar { selectedCar ->
//            selectedCar?.let { car ->
//                updateUnitsBasedOnFuelType(car.fuelType)
//                statisticsViewModel.getTripsForCar(car.id.toInt()).observe(viewLifecycleOwner) { trips ->
//                    updateChartWithTrips(trips)
//                    updateStatisticsBlocks(trips)
//                }
//            }
//        }
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

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 0.1f
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            legend.isEnabled = false
            setNoDataText("Нет данных")
        }
    }

    private fun updateChartWithTrips(trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            statsChart.clear()
            statsChart.invalidate()
            return
        }

        val entries = trips.mapIndexed { index, trip ->
            Entry(index.toFloat(), trip.fuelConsumption.toFloat())
        }

        val dataSet = LineDataSet(entries, "Расход топлива").apply {
            color = resources.getColor(R.color.blue, null)
            setDrawCircles(true)
            setDrawValues(false)
        }

        // Используем дату напрямую из TripEntity (формат "yyyy-MM-dd")
        val labels = trips.map {
            val parts = it.date.split("-")
            if (parts.size >= 3) "${parts[2]}.${parts[1]}" else it.date
        }

        statsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        statsChart.xAxis.labelCount = labels.size

        statsChart.data = LineData(dataSet)
        statsChart.invalidate()
    }

    private fun updateStatisticsBlocks(trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            statValues.forEach { it.text = "-" }
            return
        }

        val avgFuel = trips.map { it.fuelConsumption }.average()
        val avgSpeed = trips.map { it.averageSpeed }.average()
        val totalHours = trips.sumOf { it.engineHours }
        val totalDistance = trips.sumOf { it.distance }
        val totalFuel = trips.sumOf { it.fuelUsed }
        val totalCost = trips.sumOf { it.fuelCost }

        val values = listOf(avgFuel, avgSpeed, totalHours, totalDistance, totalFuel, totalCost)
        statValues.zip(values).forEach { (view, value) ->
            view.text = String.format("%.1f", value)
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
        statUnits[5].text = "₽"
    }

//    private fun bindStatBlocks() {
//        val blocks = listOf(
//            R.id.item_stat_block1, R.id.item_stat_block2, R.id.item_stat_block3,
//            R.id.item_stat_block4, R.id.item_stat_block5, R.id.item_stat_block6
//        )
//
//        statValues = blocks.map { id ->
//            binding.root.findViewById<View>(id).findViewById(R.id.stat_value1)
//        }
//
//        statUnits = blocks.map { id ->
//            binding.root.findViewById<View>(id).findViewById(R.id.stat_unit1)
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
