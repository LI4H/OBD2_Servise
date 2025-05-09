//package com.example.obd_servise.ui.statistics
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.ViewModel
//import dagger.hilt.android.lifecycle.HiltViewModel
//import javax.inject.Inject
//
//@HiltViewModel
//class StatisticsViewModel @Inject constructor(
//    private val tripDao: TripDao
//) : ViewModel() {
//
//    fun getTripsForCar(carId: Int): LiveData<List<TripEntity>> {
//        return tripDao.getTripsByCarId(carId)
//    }
//}


package com.example.obd_servise.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor() : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val _trips = MutableLiveData<List<TripEntity>>()
    val trips: LiveData<List<TripEntity>> = _trips

    fun getTripsForCar(carId: String) {
        database.getReference("cars/$carId/trips")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tripsList = mutableListOf<TripEntity>()
                    for (tripSnapshot in snapshot.children) {
                        val trip = tripSnapshot.getValue(TripEntity::class.java)
                        trip?.let {
                            // Добавляем дату из ключа (если нужно)
                            val tripWithDate = it.copy(date = tripSnapshot.key ?: "")
                            tripsList.add(tripWithDate)
                        }
                    }
                    _trips.value = tripsList.sortedBy { it.date }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })
    }
}