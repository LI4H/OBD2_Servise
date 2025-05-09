//package com.example.obd_servise.ui.statistics
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "trips")
//data class TripEntity(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val carId: Int = 0,
//    val date: Long = 0L,
//    val fuelConsumption: Double = 0.0,
//    val averageSpeed: Double = 0.0,
//    val distance: Double = 0.0,
//    val fuelUsed: Double = 0.0,
//    val fuelCost: Double = 0.0,
//    val engineHours: Double = 0.0
//) {
//    // Этот конструктор нужен Firebase
//    constructor() : this(0, 0, 0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
//}


package com.example.obd_servise.ui.statistics

data class TripEntity(
    val carId: String = "",
    val date: String = "", // Измените с Long на String
    val fuelConsumption: Double = 0.0,
    val averageSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val fuelUsed: Double = 0.0,
    val fuelCost: Double = 0.0,
    val engineHours: Double = 0.0
)