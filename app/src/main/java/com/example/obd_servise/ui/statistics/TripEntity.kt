package com.example.obd_servise.ui.statistics

data class TripEntity(
    val id: String = "",
    val carId: String = "",
    var date: String = "",
    val fuelConsumption: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val fuelUsed: Double = 0.0,
    val fuelCost: Double = 0.0,
    val engineHours: Double = 0.0,
    val fuelPrice: Double = 0.0
) {
    // Пустой конструктор для Firebase
    constructor() : this("", "", "", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}

