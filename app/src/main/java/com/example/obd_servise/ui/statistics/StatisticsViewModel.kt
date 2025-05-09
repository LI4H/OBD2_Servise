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
                            tripsList.add(it.copy(date = tripSnapshot.key ?: ""))
                        }
                    }
                    // Сортируем по дате (от новых к старым)
                    _trips.value = tripsList.sortedByDescending { it.date }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })
    }
}