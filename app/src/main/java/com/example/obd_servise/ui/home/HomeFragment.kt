package com.example.obd_servise.ui.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentHomeBinding
import com.example.obd_servise.databinding.ItemComponentBinding
import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import com.example.obd_servise.ui.car.CarPart
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.connection.ConnectionState
import com.example.obd_servise.ui.statistics.StatisticsViewModel
import com.example.obd_servise.ui.statistics.TripEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.BroadcastReceiver
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import kotlin.random.Random

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()
    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private lateinit var miniChart: LineChart
    private lateinit var componentsAdapter: ComponentsAdapter
    private val componentsList = mutableListOf<CarPart>()

    // Флаг для блокировки повторных нажатий
    private var clickLock = false
    private val clickDebouncePeriod = 500L // 0.5 секунды

    private val demoHandler = Handler(Looper.getMainLooper())
    private var demoRunnable: Runnable? = null
    private var startTime = System.currentTimeMillis()
    private var isDemo2Mode = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Регистрируем BroadcastReceiver для батареи
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        requireActivity().registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                updatePhoneBattery(level)
            }
        }, batteryFilter)
        // Инициализация мини-графика
        miniChart = binding.statsChart
        setupMiniChart()
        setupComponentsRecyclerView()

        Log.d("Navigation", "HomeFragment created")

        setupMiniChartData()
        setupClickListeners()
        setupObservers()
        loadSelectedCar()
        fetchComponents()
    }


    private fun updateMiniStats(speed: Int, rpm: Int, temp: Int) {
        // Check if binding is available
        if (_binding == null) return

        binding.miniSpeedText.text = if (speed >= 0) speed.toString() else "--"
        binding.miniRpmText.text = if (rpm >= 0) rpm.toString() else "--"
        binding.miniTempText.text = if (temp >= 0) temp.toString() else "--"
    }

    private fun updateUI(isDemo: Boolean?, elmState: ConnectionState?, ecuState: ConnectionState?) {
        when {
            isDemo == true -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.VISIBLE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.demoBtn)
                binding.statusECU.text = getString(R.string.demoBtn)

                // Запускаем DEMO режим для показателей
                startDemoMode()
            }

            elmState == ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.VISIBLE
                binding.statusELM.text = getString(R.string.connected_to_elm)
                binding.statusECU.text = getString(R.string.connected_to_ecu)

                // Запускаем DEMO2 режим (имитация холостого хода)
                startDemo2Mode()
            }

            elmState == ConnectionState.CONNECTED && ecuState != ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.connected_to_elm)
                binding.statusECU.text = getString(R.string.connecting_to_ecu)

                // Показываем нулевые значения при частичном подключении
                updateMiniStats(0, 0, 0)
            }

            elmState != ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> {
                binding.connectBtn.visibility = View.GONE
                binding.demoBtn.visibility = View.GONE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.connecting_to_elm)
                binding.statusECU.text = getString(R.string.connected_to_ecu)

                // Показываем нулевые значения при частичном подключении
                updateMiniStats(0, 0, 0)
            }

            else -> {
                binding.connectBtn.visibility = View.VISIBLE
                binding.demoBtn.visibility = View.VISIBLE
                binding.exitDemoBtn.visibility = View.GONE
                binding.exitConnectBtn.visibility = View.GONE
                binding.statusELM.text = getString(R.string.statusELM)
                binding.statusECU.text = getString(R.string.statusECU)

                // Останавливаем демо-режим и показываем "--"
                stopDemoMode()
                updateMiniStats(-1, -1, -1)
            }
        }
    }

    private fun fetchComponents() {
        carViewModel.getSelectedCar { car ->
            car?.let {
                val partsRef = FirebaseDatabase.getInstance().getReference("cars/${car.id}/parts")
                partsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        componentsList.clear()
                        for (partSnapshot in snapshot.children) {
                            val part = partSnapshot.getValue(CarPart::class.java)
                            part?.let { componentsList.add(it) }
                        }
                        componentsAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeFragment", "Error loading components: ${error.message}")
                    }
                })
            }
        }
    }

    private fun startDemoMode() {
        stopDemoMode() // Останавливаем предыдущий режим
        isDemo2Mode = false

        // Имитация движения по трассе (~120 км/ч)
        demoRunnable = object : Runnable {
            override fun run() {
                val baseSpeed = 120
                val speedVariation = Random.nextInt(-10, 10)
                val currentSpeed = baseSpeed + speedVariation

                val baseRpm = 2500
                val rpmVariation = Random.nextInt(-200, 200)
                val currentRpm = baseRpm + rpmVariation

                val engineTemp = 90 + Random.nextInt(-2, 2)

                updateMiniStats(currentSpeed, currentRpm, engineTemp)
                demoHandler.postDelayed(this, 1000)
            }
        }
        demoHandler.post(demoRunnable!!)
    }

    private fun startDemo2Mode() {
        stopDemoMode() // Останавливаем предыдущий режим
        isDemo2Mode = true

        // Имитация холостого хода
        demoRunnable = object : Runnable {
            override fun run() {
                val baseRpm = 800
                val rpmNoise = Random.nextInt(-20, 20)
                val currentRpm = baseRpm + rpmNoise
                val engineTemp = 70 + Random.nextInt(-2, 2)

                updateMiniStats(0, currentRpm, engineTemp)
                demoHandler.postDelayed(this, 1000)
            }
        }
        demoHandler.post(demoRunnable!!)
    }

    private fun stopDemoMode() {
        demoHandler.removeCallbacksAndMessages(null)
        demoRunnable = null
    }


    private fun setupComponentsRecyclerView() {
        componentsAdapter = ComponentsAdapter(componentsList)
        binding.componentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = componentsAdapter
            setHasFixedSize(true)
        }
    }


    private fun setupMiniChart() {
        miniChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            legend.isEnabled = false
            setNoDataText("Нет данных")

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 1f
                textColor = Color.WHITE
                textSize = 10f
                axisLineColor = Color.WHITE
                axisLineWidth = 1f
                setDrawAxisLine(true)
                setDrawLabels(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return value.toInt().toString()
                    }
                }
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.WHITE
                textSize = 10f
                axisLineColor = Color.WHITE
                axisLineWidth = 1f
                setDrawAxisLine(true)
                setDrawLabels(true)
                labelRotationAngle = -45f
                setAvoidFirstLastClipping(true)

                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        val tripsCount =
                            (data as? LineData)?.dataSets?.firstOrNull()?.entryCount ?: 0
                        return if (index in 0 until tripsCount) {
                            val entry = data.getDataSetByIndex(0).getEntryForIndex(index)
                            val dateInfo = entry.data?.toString() ?: ""
                            if (dateInfo.startsWith("Дата: ")) {
                                dateInfo.substring(6)
                            } else {
                                dateInfo
                            }
                        } else ""
                    }
                }
            }
        }
    }

    private fun setupMiniChartData() {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    if (!trips.isNullOrEmpty()) {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -30)
                        val filterDate = calendar.time

                        val filteredTrips = trips.filter { trip ->
                            try {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val tripDate = dateFormat.parse(trip.date)
                                tripDate?.after(filterDate) ?: false
                            } catch (e: Exception) {
                                false
                            }
                        }.sortedBy { it.date }

                        updateMiniChart(filteredTrips)
                    } else {
                        miniChart.clear()
                        miniChart.invalidate()
                    }
                }
            }
        }
    }

    private fun updateMiniChart(trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            miniChart.clear()
            miniChart.invalidate()
            miniChart.setNoDataText("Нет данных за последние 30 дней")
            return
        }

        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#673AB7"),
            Color.parseColor("#8BC34A")
        )

        val fields = listOf(
            "fuelConsumption", "avgSpeed", "engineHours",
            "distance", "fuelUsed", "fuelCost"
        )

        val dataSets = fields.mapIndexed { index, field ->
            val entries = trips.mapIndexed { tripIndex, trip ->
                val value = when (field) {
                    "fuelConsumption" -> trip.fuelConsumption.toFloat()
                    "avgSpeed" -> trip.avgSpeed.toFloat()
                    "engineHours" -> trip.engineHours.toFloat()
                    "distance" -> trip.distance.toFloat()
                    "fuelUsed" -> trip.fuelUsed.toFloat()
                    "fuelCost" -> trip.fuelCost.toFloat()
                    else -> 0f
                }
                Entry(tripIndex.toFloat(), value).apply {
                    data = "Дата: ${formatDate(trip.date)}"
                }
            }

            LineDataSet(entries, field).apply {
                color = colors[index]
                lineWidth = 2f
                setDrawCircles(true)
                setDrawValues(false)
                circleRadius = 3f
                circleHoleRadius = 1.5f
                setCircleColor(colors[index])
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
        }

        miniChart.data = LineData(dataSets)
        miniChart.invalidate()

        if (trips.size > 10) {
            val scaleX = trips.size / 10f
            miniChart.viewPortHandler.setMaximumScaleX(scaleX)
            miniChart.setVisibleXRangeMaximum(10f)
            miniChart.moveViewToX(trips.size.toFloat())
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun setupClickListeners() {
        binding.connectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceSelectionFragment)
        }

        binding.demoBtn.setOnClickListener {
            homeViewModel.enableDemoMode()
            sharedViewModel.updateElmStatus(ConnectionState.DEMO)
            sharedViewModel.updateEcuStatus(ConnectionState.DEMO)
        }

        binding.exitDemoBtn.setOnClickListener {
            homeViewModel.disableDemoMode()
            sharedViewModel.updateElmStatus(ConnectionState.DISCONNECTED)
            sharedViewModel.updateEcuStatus(ConnectionState.DISCONNECTED)
        }

        binding.exitConnectBtn.setOnClickListener {
            sharedViewModel.updateElmStatus(ConnectionState.DISCONNECTED)
            sharedViewModel.updateEcuStatus(ConnectionState.DISCONNECTED)
            homeViewModel.disableConnect()
        }
    }

    private fun setupObservers() {
        sharedViewModel.elmStatus.observe(viewLifecycleOwner) { elmState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                elmState,
                sharedViewModel.ecuStatus.value
            )
        }

        sharedViewModel.ecuStatus.observe(viewLifecycleOwner) { ecuState ->
            updateUI(
                homeViewModel.isDemoActive.value == true,
                sharedViewModel.elmStatus.value,
                ecuState
            )
        }
    }


    private fun updatePhoneBattery(level: Int) {
        binding.phoneBatteryText.text = "$level%"

        // Устанавливаем соответствующую иконку
        val batteryIcon = when {
            level <= 20 -> R.drawable.ic_battery_20
            level <= 50 -> R.drawable.ic_battery_50
            level <= 80 -> R.drawable.ic_battery_80
            else -> R.drawable.ic_battery_100
        }

        binding.batteryIcon.setImageResource(batteryIcon)

        // Меняем цвет текста в зависимости от уровня заряда
        val color = when {
            level < 20 -> ContextCompat.getColor(requireContext(), R.color.red_light)
            level < 50 -> ContextCompat.getColor(requireContext(), R.color.orange_light)
            else -> ContextCompat.getColor(requireContext(), R.color.green_light)
        }
        binding.phoneBatteryText.setTextColor(color)
    }
    private fun loadSelectedCar() {
        Log.d("HomeFragment", "Loading selected car")
        carViewModel.getSelectedCar { car ->
            car?.let {
                Log.d("HomeFragment", "Car selected: ${car.name}")
                binding.carNameText.text = it.name
                binding.carBrandText.text = "Brand: ${it.brand}"
                binding.carModelText.text = "Model: ${it.model}"
                binding.carMileageText.text = "Mileage: ${it.mileage} km"
                binding.carFuelTypeText.text = "Fuel: ${it.fuelType}"
                binding.carVinText.text = "VIN: ${it.vin}"

                if (homeViewModel.shouldNavigateToCarFragment) {
                    homeViewModel.shouldNavigateToCarFragment = false
                    findNavController().navigate(R.id.action_homeFragment_to_carFragment)
                }
            } ?: run {
                Log.d("HomeFragment", "No car selected, showing placeholders")
                binding.carNameText.text = "No car selected"
                binding.carBrandText.text = "Brand: --"
                binding.carModelText.text = "Model: --"
                binding.carMileageText.text = "Mileage: -- km"
                binding.carFuelTypeText.text = "Fuel: --"
                binding.carVinText.text = "VIN: --"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopDemoMode()
    }
    override fun onResume() {
        super.onResume()
        Log.d("Navigation", "HomeFragment onResume")
        loadSelectedCar()

        // Restart demo mode if needed
        if (homeViewModel.isDemoActive.value == true) {
            startDemoMode()
        } else if (sharedViewModel.elmStatus.value == ConnectionState.CONNECTED &&
            sharedViewModel.ecuStatus.value == ConnectionState.CONNECTED
        ) {
            startDemo2Mode()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        demoHandler.removeCallbacksAndMessages(null) // Stop all handlers
        _binding = null // Clear the binding
    }

    inner class ComponentsAdapter(private val parts: List<CarPart>) :
        RecyclerView.Adapter<ComponentsAdapter.ComponentViewHolder>() {

        inner class ComponentViewHolder(private val binding: ItemComponentBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(part: CarPart) {
                with(binding) {
                    componentName.text = part.name

                    when (part.condition) {
                        "normal" -> {
                            componentStatus.text = "NORMAL"
                            componentStatus.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.green_light
                                )
                            )
                        }

                        "warning" -> {
                            componentStatus.text = "WARNING"
                            componentStatus.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.orange_light
                                )
                            )
                        }

                        "critical" -> {
                            componentStatus.text = "CRITICAL"
                            componentStatus.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.red_light
                                )
                            )
                        }

                        else -> {
                            componentStatus.text = "UNKNOWN"
                            componentStatus.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    android.R.color.white
                                )
                            )
                        }
                    }

                    root.setOnClickListener {
                        Toast.makeText(
                            itemView.context,
                            "Текущий: ${part.currentMileage} км\nРекомендуемый: ${part.recommendedMileage} км",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder {
            val binding = ItemComponentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ComponentViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) {
            holder.bind(parts[position])
        }

        override fun getItemCount(): Int = parts.size
    }
}