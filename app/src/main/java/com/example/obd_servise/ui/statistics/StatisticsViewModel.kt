package com.example.obd_servise.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@HiltViewModel
class StatisticsViewModel @Inject constructor() : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference


    // Изменяем тип на MutableLiveData и инициализируем пустым списком
    private val _trips = MutableLiveData<List<TripEntity>>(emptyList())

    // Публичное LiveData для наблюдения
    val trips: LiveData<List<TripEntity>> = _trips

    // Новый метод для получения поездки из уже загруженного списка
    fun getTripFromLoadedList(carId: String, tripId: String): TripEntity? {
        return _trips.value?.firstOrNull { it.id == tripId && it.carId == carId }
    }

    // Модифицированный метод getTripDetails
    fun getTripDetails(carId: String, tripId: String): LiveData<TripEntity?> {
        val result = MutableLiveData<TripEntity?>()

        // Сначала проверяем локально загруженные данные
        val localTrip = getTripFromLoadedList(carId, tripId)
        if (localTrip != null) {
            result.value = localTrip
            return result
        }

        // Если локально не нашли, запрашиваем из Firebase
        database.child("cars/$carId/trips")
            .orderByChild("id")
            .equalTo(tripId)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    result.value = snapshot.children.firstOrNull()?.getValue(TripEntity::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    result.value = null
                }
            })
        return result
    }

    fun updateTrip(trip: TripEntity): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        database.child("cars/${trip.carId}/trips")
            .orderByChild("id")
            .equalTo(trip.id)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.first().ref.setValue(trip)
                            .addOnCompleteListener {
                                result.value = it.isSuccessful
                            }
                    } else {
                        result.value = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    result.value = false
                }
            })

        return result
    }

    fun deleteTrip(carId: String, tripId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        database.child("cars/$carId/trips")
            .orderByChild("id")
            .equalTo(tripId)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.first().ref.removeValue()
                            .addOnCompleteListener {
                                result.value = it.isSuccessful
                            }
                    } else {
                        result.value = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    result.value = false
                }
            })

        return result
    }

    // Ваш существующий метод getTripsForCar
    fun getTripsForCar(carId: String) {
        database.child("cars/$carId/trips")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tripsList = mutableListOf<TripEntity>()

                    for (tripSnapshot in snapshot.children) {
                        val trip = tripSnapshot.getValue(TripEntity::class.java)
                        trip?.let {
                            tripsList.add(it)
                        }
                    }

                    // Сортируем по дате (новые сначала) и обновляем LiveData
                    _trips.value = tripsList.sortedByDescending {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // В случае ошибки просто сохраняем пустой список
                    _trips.value = emptyList()
                }
            })
    }
}