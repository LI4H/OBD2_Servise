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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.R

import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import com.example.obd_servise.ui.car.Car
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.connection.ConnectionState
import com.example.obd_servise.ui.home.HomeViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.sin
import kotlin.random.Random

class DynamicsFragment : Fragment() {
    // UI элементы
    private lateinit var carNameText: TextView
    private lateinit var carBrandText: TextView
    private lateinit var carModelText: TextView
    private lateinit var carMileageText: TextView
    private lateinit var carFuelTypeText: TextView
    private lateinit var carVinText: TextView
    private lateinit var statusText: TextView

    private lateinit var speedTextView: TextView
    private lateinit var rpmTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var fuelTextView: TextView
    private lateinit var accelerationTextView: TextView
    private lateinit var voltageTextView: TextView
    private lateinit var uptimeTextView: TextView
    private lateinit var speedProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var lineChart: LineChart

    // ViewModels
    private lateinit var carViewModel: CarViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var homeViewModel: HomeViewModel

    // Состояния
    private val demoHandler = Handler(Looper.getMainLooper())
    private var demoRunnable: Runnable? = null
    private val chartData = mutableListOf<Entry>()
    private var timeCounter = 0f
    private val maxDataPoints = 100
    private var startTime = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dynamics, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        // Инициализация ViewModels
        carViewModel = ViewModelProvider(requireActivity()).get(CarViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        // Инициализация UI элементов
        carNameText = view.findViewById(R.id.carNameText)
        carBrandText = view.findViewById(R.id.carBrandText)
        carModelText = view.findViewById(R.id.carModelText)
        carMileageText = view.findViewById(R.id.carMileageText)
        carFuelTypeText = view.findViewById(R.id.carFuelTypeText)
        carVinText = view.findViewById(R.id.carVinText)
        statusText = view.findViewById(R.id.statusText)

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
        setupObservers()
        loadCarData()
    }

    private fun setupObservers() {
        // Наблюдаем за состоянием демо-режима
        homeViewModel.isDemoActive.observe(viewLifecycleOwner) { isDemo ->
            updateConnectionStatus(
                isDemo,
                sharedViewModel.elmStatus.value,
                sharedViewModel.ecuStatus.value
            )
        }

        // Наблюдаем за состоянием подключения к ELM327
        sharedViewModel.elmStatus.observe(viewLifecycleOwner) { elmState ->
            updateConnectionStatus(
                homeViewModel.isDemoActive.value == true,
                elmState,
                sharedViewModel.ecuStatus.value
            )
        }

        // Наблюдаем за состоянием подключения к ЭБУ
        sharedViewModel.ecuStatus.observe(viewLifecycleOwner) { ecuState ->
            updateConnectionStatus(
                homeViewModel.isDemoActive.value == true,
                sharedViewModel.elmStatus.value,
                ecuState
            )
        }
    }

    private fun updateConnectionStatus(
        isDemo: Boolean?,
        elmState: ConnectionState?,
        ecuState: ConnectionState?
    ) {
        when {
            isDemo == true -> {
                // Демо-режим активен
                statusText.text = "DEMO"
                statusText.setTextColor(Color.YELLOW)
                startDemoMode()
            }

            elmState == ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> {
                // Полное подключение
                statusText.text = "CONNECTED"
                statusText.setTextColor(Color.GREEN)
                startConnectedMode()
            }

            else -> {
                // Нет подключения
                statusText.text = "DISCONNECTED"
                statusText.setTextColor(Color.RED)
                showDisconnectedState()
            }
        }
    }

    private fun loadCarData() {
        carViewModel.getSelectedCar { car ->
            if (car != null) {
                updateCarInfo(car)
            } else {
                Log.d("DynamicsFragment", "No selected car found")
            }
        }
    }

    private fun updateCarInfo(car: Car) {
        carNameText.text = car.name
        carBrandText.text = "Brand: ${car.brand}"
        carModelText.text = "Model: ${car.model}"
        carMileageText.text = "Mileage: ${car.mileage} km"
        carFuelTypeText.text = "Fuel: ${car.fuelType}"
        carVinText.text = "VIN: ${car.vin}"
    }

    private fun showDisconnectedState() {
        demoHandler.removeCallbacksAndMessages(null)

        speedTextView.text = "--"
        rpmTextView.text = "--"
        tempTextView.text = "--°C"
        fuelTextView.text = "--%"
        voltageTextView.text = "--V"
        accelerationTextView.text = "-- м/с²"
        speedProgress.progress = 0
        uptimeTextView.text = "00:00:00"

        chartData.clear()
        lineChart.clear()
    }

    private fun startConnectedMode() {
        demoHandler.removeCallbacksAndMessages(null)

        // Здесь должна быть логика реального подключения к OBD
        // Пока используем демо-режим как заглушку
        startDemo2Mode()
    }

    private fun startDemoMode() {
        demoHandler.removeCallbacksAndMessages(null)

        // Имитация движения по трассе (~120 км/ч)
        demoRunnable = object : Runnable {
            override fun run() {
                val baseSpeed = 120
                val speedVariation = Random.nextInt(-10, 10)
                val currentSpeed = baseSpeed + speedVariation

                val baseRpm = 2500
                val rpmVariation = Random.nextInt(-200, 200)
                val currentRpm = baseRpm + rpmVariation

                val engineTemp = 90 + Random.nextDouble(-2.0, 2.0)
                val voltage = 13.8 + Random.nextDouble(-0.2, 0.2)
                val fuelLevel = 65.0 - (System.currentTimeMillis() % 10000) / 10000.0 * 0.1
                val acceleration = 0.1 + Random.nextDouble(-0.2, 0.2)

                speedTextView.text = currentSpeed.toString()
                rpmTextView.text = currentRpm.toString()
                tempTextView.text = "%.1f°C".format(engineTemp)
                fuelTextView.text = "%.1f%%".format(fuelLevel)
                voltageTextView.text = "%.1fV".format(voltage)
                accelerationTextView.text = "%.1f м/с²".format(acceleration)
                speedProgress.progress = (currentSpeed * 100 / 220)
                updateUptime()

                demoHandler.postDelayed(this, 1000)
            }
        }
        demoHandler.post(demoRunnable!!)

        // Обновление графика
        demoHandler.post(object : Runnable {
            override fun run() {
                updateChartData(2500f, 500f)
                demoHandler.postDelayed(this, 500)
            }
        })
    }

    private fun startDemo2Mode() {
        // Имитация холостого хода (как было ранее)
        demoRunnable = object : Runnable {
            override fun run() {
                val baseRpm = 800
                val rpmNoise = Random.nextInt(-20, 20)
                val currentRpm = baseRpm + rpmNoise
                val engineTemp = 70 + Random.nextDouble(-2.0, 2.0)
                val voltage = 13.8 + Random.nextDouble(-0.2, 0.2)
                val fuelLevel = 85.0 - (System.currentTimeMillis() % 10000) / 10000.0 * 0.1

                speedTextView.text = "0"
                rpmTextView.text = currentRpm.toString()
                tempTextView.text = "%.1f°C".format(engineTemp)
                fuelTextView.text = "%.1f%%".format(fuelLevel)
                voltageTextView.text = "%.1fV".format(voltage)
                accelerationTextView.text = "0.0 м/с²"
                speedProgress.progress = 0
                updateUptime()

                demoHandler.postDelayed(this, 1000)
            }
        }
        demoHandler.post(demoRunnable!!)

        demoHandler.post(object : Runnable {
            override fun run() {
                updateChartData(800f, 50f) // Базовые значения для DEMO2 режима
                demoHandler.postDelayed(this, 500)
            }
        })
    }

    private fun updateUptime() {
        val uptimeSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
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
        leftAxis.axisMinimum = 0f

        lineChart.axisRight.isEnabled = false
        lineChart.data = LineData(LineDataSet(null, "RPM").apply {
            color = Color.GREEN
            setCircleColor(Color.GREEN)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
        })
    }

    private fun updateChartData(baseValue: Float, variationRange: Float) {
        val value =
            baseValue + sin(timeCounter * 0.1) * variationRange / 2 + Random.nextFloat() * variationRange / 2
        chartData.add(Entry(timeCounter, value.toFloat()))

        if (chartData.size > maxDataPoints) {
            chartData.removeAt(0)
        }

        val dataSet = lineChart.data.getDataSetByIndex(0) as LineDataSet
        dataSet.values = chartData
        dataSet.notifyDataSetChanged()
        lineChart.data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()

        lineChart.axisLeft.axisMaximum = baseValue + variationRange * 1.5f
        lineChart.axisLeft.axisMinimum = maxOf(0f, baseValue - variationRange * 1.5f)

        timeCounter += 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        demoHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun newInstance(): DynamicsFragment {
            return DynamicsFragment()
        }
    }
}