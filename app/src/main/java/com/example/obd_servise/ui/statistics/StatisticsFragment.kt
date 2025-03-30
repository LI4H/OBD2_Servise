package com.example.obd_servise.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels  // Изменено на activityViewModels
import androidx.fragment.app.viewModels
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentStatisticsBinding
import com.example.obd_servise.ui.home.HomeViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.random.Random
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var statsChart: LineChart

    // Объявляем переменные для каждого блока статистики
    private lateinit var statTitle1: TextView
    private lateinit var statValue1: TextView
    private lateinit var statUnit1: TextView

    private lateinit var statTitle2: TextView
    private lateinit var statValue2: TextView
    private lateinit var statUnit2: TextView

    private lateinit var statTitle3: TextView
    private lateinit var statValue3: TextView
    private lateinit var statUnit3: TextView

    private lateinit var statTitle4: TextView
    private lateinit var statValue4: TextView
    private lateinit var statUnit4: TextView

    private lateinit var statTitle5: TextView
    private lateinit var statValue5: TextView
    private lateinit var statUnit5: TextView

    private lateinit var statTitle6: TextView
    private lateinit var statValue6: TextView
    private lateinit var statUnit6: TextView

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()  // Используем activityViewModels для получения общей ViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        // Инициализация графика
        statsChart = binding.root.findViewById(R.id.stats_chart)

        statsChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            // Настройка осей
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 1f
            axisLeft.granularity = 0.1f // Шаг 0.1 для оси Y

            axisRight.setDrawGridLines(false)
            axisRight.isEnabled = false

            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f // Шаг 1 для оси X

            // Установка подписей для оси X
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val months = listOf(
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
            )
            val labels = (currentMonth downTo currentMonth - 11).map {
                months[(it + 12) % 12]
            }.reversed()

            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size // Ровно 12 месяцев

            // Удаление легенды (цветные кубики)
            legend.isEnabled = false // Отключение отображения легенды

            // Настройка пустого состояния
            setNoDataText("")
        }


        // Установим фильтр на 7 дней по умолчанию
        //   val filterGroup = binding.filterGroup
        //   filterGroup.check(R.id.radio_7_days)  // Устанавливаем фильтр на 7 дней по умолчанию
        // Наблюдаем за статусом демо-режима и обновляем график
        homeViewModel.isDemoActive.observe(viewLifecycleOwner, { isDemoActive ->
            if (isDemoActive) {
                updateChartWithDemoData()
                statisticsViewModel.demoModeEnabled()
            } else {
                clearChart()   // Устанавливаем фиктивные данные вместо полного очищения графика
                val entries = listOf(Entry(0f, 0f))
                val dataSet = LineDataSet(entries, "No Data").apply {
                    color = Color.GRAY
                    setDrawCircles(false)
                    setDrawValues(false)
                }
                statsChart.data = LineData(dataSet)
                statsChart.invalidate() // Перерисовываем график
                statisticsViewModel.demoModeDisabled()
            }
        })

        // Привязка блоков статистики через <include>
        val statBlock1 = binding.root.findViewById<View>(R.id.item_stat_block1)
        val statBlock2 = binding.root.findViewById<View>(R.id.item_stat_block2)
        val statBlock3 = binding.root.findViewById<View>(R.id.item_stat_block3)
        val statBlock4 = binding.root.findViewById<View>(R.id.item_stat_block4)
        val statBlock5 = binding.root.findViewById<View>(R.id.item_stat_block5)
        val statBlock6 = binding.root.findViewById<View>(R.id.item_stat_block6)

        // Привязываем элементы внутри каждого блока
        statTitle1 = statBlock1.findViewById(R.id.stat_title1)
        statValue1 = statBlock1.findViewById(R.id.stat_value1)
        statUnit1 = statBlock1.findViewById(R.id.stat_unit1)

        statTitle2 = statBlock2.findViewById(R.id.stat_title2)
        statValue2 = statBlock2.findViewById(R.id.stat_value2)
        statUnit2 = statBlock2.findViewById(R.id.stat_unit2)

        statTitle3 = statBlock3.findViewById(R.id.stat_title3)
        statValue3 = statBlock3.findViewById(R.id.stat_value3)
        statUnit3 = statBlock3.findViewById(R.id.stat_unit3)

        statTitle4 = statBlock4.findViewById(R.id.stat_title4)
        statValue4 = statBlock4.findViewById(R.id.stat_value4)
        statUnit4 = statBlock4.findViewById(R.id.stat_unit4)

        statTitle5 = statBlock5.findViewById(R.id.stat_title5)
        statValue5 = statBlock5.findViewById(R.id.stat_value5)
        statUnit5 = statBlock5.findViewById(R.id.stat_unit5)

        statTitle6 = statBlock6.findViewById(R.id.stat_title6)
        statValue6 = statBlock6.findViewById(R.id.stat_value6)
        statUnit6 = statBlock6.findViewById(R.id.stat_unit6)

        // Наблюдаем за изменениями в модели
        statisticsViewModel.fuelConsumption.observe(viewLifecycleOwner, { fuelConsumption ->
            statValue1.text = fuelConsumption.toString()
        })

        statisticsViewModel.averageSpeed.observe(viewLifecycleOwner, { averageSpeed ->
            statValue2.text = averageSpeed.toString()
        })

        statisticsViewModel.engineHours.observe(viewLifecycleOwner, { engineHours ->
            statValue3.text = engineHours.toString()
        })

        statisticsViewModel.distance.observe(viewLifecycleOwner, { distance ->
            statValue4.text = distance.toString()
        })

        statisticsViewModel.fuelUsed.observe(viewLifecycleOwner, { fuelUsed ->
            statValue5.text = fuelUsed.toString()
        })

        statisticsViewModel.fuelCost.observe(viewLifecycleOwner, { fuelCost ->
            statValue6.text = fuelCost.toString()
        })

    }

    private fun updateChartWithDemoData() {
        // Генерация случайных данных для графика
        val entries1 = mutableListOf<Entry>()
        val entries2 = mutableListOf<Entry>()
        val entries3 = mutableListOf<Entry>()
        val entries4 = mutableListOf<Entry>()
        val entries5 = mutableListOf<Entry>()
        val entries6 = mutableListOf<Entry>()

        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        for (i in 0 until 7) {  // Создаем 10 точек данных для каждой линии
            val x = i.toFloat()
            val date = sdf.format(calendar.time)
            // Разные случайные значения для каждой линии
            entries1.add(Entry(x, Random.nextDouble(0.0, 1.0).toFloat()))  // Для расхода топлива
            entries2.add(Entry(x, Random.nextDouble(0.0, 1.0).toFloat()))  // Для скорости
            entries3.add(Entry(x, Random.nextDouble(0.0, 1.0).toFloat()))    // Для моточасов
            entries4.add(Entry(x, Random.nextDouble(0.0, 1.0).toFloat()))  // Для пройденного пути
            entries5.add(
                Entry(
                    x,
                    Random.nextDouble(0.0, 1.0).toFloat()
                )
            )    // Для использованного топлива
            entries6.add(Entry(x, Random.nextDouble(0.0, 1.0).toFloat())) // Для стоимости топлива


            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Создание наборов данных для линий с разными цветами
        val dataSet1 = LineDataSet(entries1, "Расход топлива").apply {
            color = resources.getColor(R.color.blue)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        val dataSet2 = LineDataSet(entries2, "Средняя скорость").apply {
            color = resources.getColor(R.color.orange)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        val dataSet3 = LineDataSet(entries3, "Моточасы").apply {
            color = resources.getColor(R.color.yellow)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        val dataSet4 = LineDataSet(entries4, "Пройденный путь").apply {
            color = resources.getColor(R.color.red)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        val dataSet5 = LineDataSet(entries5, "Использовано топлива").apply {
            color = resources.getColor(R.color.purple)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        val dataSet6 = LineDataSet(entries6, "Стоимость топлива").apply {
            color = resources.getColor(R.color.green)
            valueTextColor = resources.getColor(R.color.white)
            lineWidth = 4f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.colorPrimary))
            circleRadius = 2f
        }

        // Добавляем данные в график
        val lineData = LineData(dataSet1, dataSet2, dataSet3, dataSet4, dataSet5, dataSet6)
        statsChart.data = lineData
        statsChart.invalidate()  // Перерисовываем график
    }

    private fun clearChart() {
        statsChart.clear()  // Очищаем график
        statsChart.invalidate()  // Перерисовываем
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
