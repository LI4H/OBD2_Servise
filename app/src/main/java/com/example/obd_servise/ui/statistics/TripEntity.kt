package com.example.obd_servise.ui.statistics

data class TripEntity(
    val carId: String = "",
    val date: String = "",
    val fuelConsumption: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val fuelUsed: Double = 0.0,
    val fuelCost: Double = 0.0,
    val engineHours: Double = 0.0
)