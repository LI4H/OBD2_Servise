//package com.example.obd_servise.ui.statistics
//
//import androidx.lifecycle.LiveData
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//
//@Dao
//interface TripDao {
//    @Query("SELECT * FROM trips WHERE carId = :carId ORDER BY date ASC")
//    fun getTripsByCarId(carId: Int): LiveData<List<TripEntity>>
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTrip(trip: TripEntity)
//
////    @Query("SELECT * FROM trips")
////    suspend fun getAllTrips(): List<TripEntity>
//
//}
