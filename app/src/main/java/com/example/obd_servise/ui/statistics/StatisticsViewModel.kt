package com.example.obd_servise.ui.statistics

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class StatisticsViewModel : ViewModel() {

    // Показатели
    private val _fuelConsumption = MutableLiveData(0.0)
    val fuelConsumption: LiveData<Double> get() = _fuelConsumption

    private val _averageSpeed = MutableLiveData(0.0)
    val averageSpeed: LiveData<Double> get() = _averageSpeed

    private val _engineHours = MutableLiveData(0.0)
    val engineHours: LiveData<Double> get() = _engineHours

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    private val _fuelUsed = MutableLiveData(0.0)
    val fuelUsed: LiveData<Double> get() = _fuelUsed

    private val _fuelCost = MutableLiveData(0.0)
    val fuelCost: LiveData<Double> get() = _fuelCost

    // История данных для демо-режима
    private val demoHistory = mutableListOf<DemoData>()

    // Логика демо-режима
    fun demoModeEnabled() {

        val randomData = generateRandomData()
        demoHistory.add(randomData)

        // Ограничиваем количество символов после запятой до 1
        _fuelConsumption.value = formatDouble(randomData.fuelConsumption)
        _averageSpeed.value = formatDouble(randomData.averageSpeed)
        _distance.value = formatDouble(randomData.distance)
        _fuelUsed.value = formatDouble(randomData.fuelUsed)
        _fuelCost.value = formatDouble(randomData.fuelCost)
        _engineHours.value = formatDouble(randomData.engineHours)

        Log.d("StatisticsViewModel", "Demo Mode Enabled: $randomData")

    }

    private fun generateRandomData(): DemoData {
        val fuelConsumption = Random.nextDouble(5.0, 12.0)
        val averageSpeed = Random.nextDouble(30.0, 90.0)
        val distance = Random.nextDouble(10.0, 500.0)
        val fuelUsed = (fuelConsumption * distance) / 100
        val fuelCost = fuelUsed * 2.5  // Цена топлива 2.5 Br за литр
        val engineHours = distance / averageSpeed
        return DemoData(fuelConsumption, averageSpeed, distance, fuelUsed, fuelCost, engineHours)
    }

    // Для сброса демо-режима
    fun demoModeDisabled() {
        _fuelConsumption.value = 0.0
        _averageSpeed.value = 0.0
        _distance.value = 0.0
        _fuelUsed.value = 0.0
        _fuelCost.value = 0.0
        _engineHours.value = 0.0
        Log.d("StatisticsViewModel", "Demo Mode Disabled")
    }


    // Функция для форматирования числа
    private fun formatDouble(value: Any?): Double {
        return try {
            when (value) {
                is String -> value.replace(",", ".").toDouble()
                is Double -> String.format("%.1f", value).replace(",", ".").toDouble()
                is Float -> String.format("%.1f", value).replace(",", ".").toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                else -> {
                    Log.e(
                        "StatisticsViewModel",
                        "Unexpected type: ${value?.javaClass?.simpleName}, value: $value"
                    )
                    0.0
                }
            }
        } catch (e: Exception) {
            Log.e("StatisticsViewModel", "Error formatting value: $value", e)
            0.0
        }
    }
}

// Модель для хранения данных демо-режима
data class DemoData(
    val fuelConsumption: Double,
    val averageSpeed: Double,
    val distance: Double,
    val fuelUsed: Double,
    val fuelCost: Double,
    val engineHours: Double,
    val timestamp: Long = System.currentTimeMillis()  // Время, когда данные были записаны
)
