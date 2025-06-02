//package com.example.obd_servise.ui.dynamics
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import com.example.obd_servise.R
//import com.example.obd_servise.obd_connection.bluetooth.ObdManager
//import com.github.eltonvs.obd.command.engine.RPMCommand
//import com.github.eltonvs.obd.command.engine.SpeedCommand
//import kotlinx.coroutines.*
//
//class DynamicsFragment : Fragment() {
//    private lateinit var speedTextView: TextView
//    private lateinit var rpmTextView: TextView
//    private lateinit var obdManager: ObdManager
//    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_dynamics, container, false)
//        speedTextView = view.findViewById(R.id.speedTextView)
//        rpmTextView = view.findViewById(R.id.rpmTextView)
//        return view
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        obdManager = ObdManager(requireContext())
//        startReadingObdData()
//    }
//
//    private fun startReadingObdData() {
//        scope.launch(Dispatchers.IO) {
//            while (isActive) {
//                try {
//                    // Отправляем команды и получаем ответы
//                    val speedResponse = obdManager.sendCommand(SpeedCommand(), delay(200))
//                    val rpmResponse = obdManager.sendCommand(RPMCommand(), delay(200))
//
//                    // Обрабатываем ответы
//                    val speed =
//                        if (speedResponse?.value != null && !speedResponse.value.contains("ERROR")) {
//                            "${speedResponse.value} ${speedResponse.unit ?: "км/ч"}"
//                        } else {
//                            "N/A"
//                        }
//
//                    val rpm =
//                        if (rpmResponse?.value != null && !rpmResponse.value.contains("ERROR")) {
//                            "${rpmResponse.value} ${rpmResponse.unit ?: "об/мин"}"
//                        } else {
//                            "N/A"
//                        }
//
//                    // Обновляем UI
//                    withContext(Dispatchers.Main) {
//                        speedTextView.text = "$speed"
//                        rpmTextView.text = "$rpm"
//                    }
//                } catch (e: Exception) {
//                    // Логируем ошибку и обновляем UI значением "N/A"
//                    Log.e("DynamicsFragment", "Error reading OBD data", e)
//                    withContext(Dispatchers.Main) {
//                        speedTextView.text = "N/A"
//                        rpmTextView.text = "N/A"
//                    }
//                }
//
//                // Задержка перед следующим запросом
//                delay(1000)
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        scope.cancel()
//        obdManager.disconnect()
//    }
//}

package com.example.obd_servise.ui.dynamics

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.obd_servise.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

class DynamicsFragment : Fragment() {
    // UI элементы
    private lateinit var speedTextView: TextView
    private lateinit var rpmTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var fuelTextView: TextView
    private lateinit var accelerationTextView: TextView
    private lateinit var voltageTextView: TextView
    private lateinit var uptimeTextView: TextView
    private lateinit var speedProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var lineChart: LineChart

    // Демо-режим
    private var isDemoMode = true
    private val demoHandler = Handler(Looper.getMainLooper())
    private var demoRunnable: Runnable? = null

    // Данные для графика
    private val chartData = mutableListOf<Entry>()
    private var timeCounter = 0f
    private val maxDataPoints = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dynamics, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        speedTextView = view.findViewById(R.id.speedTextView)
        rpmTextView = view.findViewById(R.id.rpmTextView)
        tempTextView = view.findViewById(R.id.tempTextView)
        fuelTextView = view.findViewById(R.id.fuelTextView)
        accelerationTextView = view.findViewById(R.id.accelerationTextView)
        voltageTextView = view.findViewById(R.id.voltageTextView)
        uptimeTextView = view.findViewById(R.id.uptimeTextView)
        speedProgress = view.findViewById(R.id.speedProgress)
        lineChart = view.findViewById(R.id.metricsGraph)

        setupChart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Проверяем аргументы на наличие demoMode
        arguments?.let {
            isDemoMode = it.getBoolean("demoMode", false)
        }

        if (isDemoMode) {
            startDemoMode()
        } else {
            // Здесь можно добавить реальное подключение к OBD
        }
    }

    private fun startDemoMode() {
        // Имитация работы двигателя на холостом ходу
        demoRunnable = object : Runnable {
            override fun run() {
                updateDemoData()
                demoHandler.postDelayed(this, 1000) // Обновляем каждую секунду
            }
        }
        demoHandler.post(demoRunnable!!)

        // Запускаем обновление графика каждые 500мс
        demoHandler.post(object : Runnable {
            override fun run() {
                updateChartData()
                demoHandler.postDelayed(this, 500)
            }
        })
    }

    private fun updateDemoData() {
        // Имитация параметров двигателя на холостом ходу
        val baseRpm = 800
        val rpmNoise = (Random.nextDouble(-20.0, 20.0)).toInt()
        val currentRpm = baseRpm + rpmNoise

        // Температура двигателя (медленно растет до рабочей)
        val engineTemp = 70 + Random.nextDouble(-2.0, 2.0)

        // Напряжение бортовой сети
        val voltage = 13.8 + Random.nextDouble(-0.2, 0.2)

        // Уровень топлива (медленно уменьшается)
        val fuelLevel = 85.0 - (System.currentTimeMillis() % 10000) / 10000.0 * 0.1

        // Обновляем UI
        speedTextView.text = "0"
        rpmTextView.text = "$currentRpm"
        tempTextView.text = "%.1f°C".format(engineTemp)
        fuelTextView.text = "%.1f%%".format(fuelLevel)
        voltageTextView.text = "%.1fV".format(voltage)
        accelerationTextView.text = "0.0 м/с²"

        // Прогресс бар (0% так как скорость 0)
        speedProgress.progress = 0

        // Время работы (формат HH:MM:SS)
        val uptimeSeconds = (System.currentTimeMillis() / 1000).toInt()
        val hours = uptimeSeconds / 3600
        val minutes = (uptimeSeconds % 3600) / 60
        val seconds = uptimeSeconds % 60
        uptimeTextView.text = "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun setupChart() {
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.setBackgroundColor(Color.BLACK)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#333333")
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 700f
        leftAxis.axisMaximum = 900f

        lineChart.axisRight.isEnabled = false

        // Инициализируем пустые данные
        val dataSet = LineDataSet(null, "RPM").apply {
            color = Color.GREEN
            setCircleColor(Color.GREEN)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
        }
        lineChart.data = LineData(dataSet)
    }

    private fun updateChartData() {
        // Генерируем демо-данные для графика (имитация RPM)
        val rpmValue = 800 + sin(timeCounter * 0.1) * 20 + Random.nextFloat() * 10

        // Добавляем новую точку
        chartData.add(Entry(timeCounter, rpmValue.toFloat()))

        // Удаляем старые точки, если их слишком много
        if (chartData.size > maxDataPoints) {
            chartData.removeAt(0)
        }

        // Получаем или создаем набор данных
        val dataSet = if (lineChart.data != null && lineChart.data.dataSetCount > 0) {
            lineChart.data.getDataSetByIndex(0) as LineDataSet
        } else {
            LineDataSet(chartData, "RPM").apply {
                color = Color.GREEN
                setCircleColor(Color.GREEN)
                lineWidth = 2f
                circleRadius = 3f
                setDrawCircleHole(false)
                setDrawValues(false)
            }
        }

        // Обновляем данные
        dataSet.values = chartData
        dataSet.notifyDataSetChanged()

        if (lineChart.data == null) {
            lineChart.data = LineData(dataSet)
        } else {
            lineChart.data.notifyDataChanged()
        }

        lineChart.notifyDataSetChanged()
        lineChart.invalidate()

        // Увеличиваем счетчик времени
        timeCounter += 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Останавливаем демо-режим при закрытии фрагмента
        demoHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun newInstance(demoMode: Boolean = false): DynamicsFragment {
            return DynamicsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("demoMode", demoMode)
                }
            }
        }
    }
}