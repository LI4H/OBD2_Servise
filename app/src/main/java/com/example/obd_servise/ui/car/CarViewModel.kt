package com.example.obd_servise.ui.car

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.*

data class Car(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val vin: String = "",
    val licensePlate: String = "",
    val mileage: Int = 0,
    val fuelPrice: Float = 0f,
    val fuelType: String = "",
    val consumption: Float = 0f,

    @get:PropertyName("isSelected") @set:PropertyName("isSelected")
    var isSelected: Int = 0, // 🔥 Новое поле
    val trips: Map<String, TripEntity>? = null

)


class CarViewModel : ViewModel() {

    // --- Поля ввода автомобиля ---

    var isSelected: Int = 0
    var name: String = ""
    var brand: String = ""
    var model: String = ""
    var year: Int = 0
    var vin: String = ""
    var licensePlate: String = ""

    var mileage: Int = 0
    var fuelPrice: Float = 0f
    var fuelType: String = ""
    var consumption: Float = 0f
    fun updateCarData(brand: String, model: String, year: Int, vin: String, licensePlate: String) {
        this.brand = brand
        this.model = model
        this.year = year
        this.vin = vin
        this.licensePlate = licensePlate
    }
    // --- Firebase ---
    private val dbRef = FirebaseDatabase.getInstance().getReference("cars")

    // --- LiveData для списка автомобилей ---
    private val _carList = MutableLiveData<List<Car>>()
    val carList: LiveData<List<Car>> get() = _carList

    init {
        loadCars()
    }

    private fun loadCars() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cars = mutableListOf<Car>()
                for (child in snapshot.children) {
                    val car = child.getValue(Car::class.java)
                    car?.let {
                        cars.add(it.copy(id = child.key ?: ""))
                    }
                }
                _carList.value = cars
            }

            override fun onCancelled(error: DatabaseError) {
                // логировать при необходимости
            }
        })
    }


    fun loadCarById(carId: String, callback: (Car) -> Unit) {
        dbRef.child(carId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val car = snapshot.getValue(Car::class.java)
                car?.let {
                    callback(it.copy(id = carId))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun saveCarToFirebase(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val car = createCarFromViewModel()
        dbRef.push().setValue(car)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCarInFirebase(carId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val car = createCarFromViewModel()
        dbRef.child(carId).setValue(car)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun createCarFromViewModel(): Car {
        return Car(
            name = name,
            brand = brand,
            model = model,
            year = year,
            vin = vin,
            licensePlate = licensePlate,
            mileage = mileage,
            fuelPrice = fuelPrice,
            fuelType = fuelType,
            consumption = consumption,
            isSelected = isSelected
        )
    }
    fun getSelectedCar(callback: (Car?) -> Unit) {
        Log.d("CarViewModel", "Getting selected car")
        dbRef.orderByChild("isSelected").equalTo(1.0)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val car = snapshot.children.firstOrNull()?.getValue(Car::class.java)
                    Log.d("CarViewModel", "Selected car: ${car?.name ?: "null"}")
                    car?.let {
                        callback(it.copy(id = snapshot.children.first().key ?: ""))
                    } ?: callback(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CarViewModel", "Error getting selected car: ${error.message}")
                    callback(null)
                }
            })
    }

    fun selectCarForStats(carId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>()
            for (child in snapshot.children) {
                val key = child.key ?: continue
                updates["$key/isSelected"] = if (key == carId) 1 else 0
            }

            dbRef.updateChildren(updates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }

        }.addOnFailureListener {
            onFailure(it)
        }
    }
    fun isCarSelected(carId: String): Boolean {
        return carList.value?.firstOrNull { it.id == carId }?.isSelected == 1
    }
    fun deleteCarFromFirebase(
        carId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        dbRef.child(carId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


}
