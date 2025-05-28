package com.example.obd_servise.ui.statistics.addTrip

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.databinding.FragmentAddTripBinding
import com.example.obd_servise.ui.car.CarPart
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddTripFragment : Fragment() {

    private var _binding: FragmentAddTripBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePicker()
        binding.saveTripButton.setOnClickListener { saveTrip() }
    }

    private fun getSelectedCarId(onResult: (String?) -> Unit) {
        val carsRef = FirebaseDatabase.getInstance().getReference("cars")
        carsRef.orderByChild("isSelected").equalTo(1.0).get().addOnSuccessListener { snapshot ->
            val selectedCarSnapshot = snapshot.children.firstOrNull()
            val selectedCarId = selectedCarSnapshot?.key
            onResult(selectedCarId)
        }.addOnFailureListener {
            Log.e("AddTripFragment", "Ошибка получения выбранного авто", it)
            onResult(null)
        }
    }

    private fun setupDatePicker() {
        binding.dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = format.format(calendar.time)
                    binding.dateEditText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveTrip() {
        val distance = binding.distanceEditText.text.toString().toDoubleOrNull()
        val fuelUsed = binding.fuelEditText.text.toString().toDoubleOrNull()
        val engineHours = binding.hoursEditText.text.toString().toDoubleOrNull()

        if (selectedDate.isEmpty() || distance == null || fuelUsed == null || engineHours == null || engineHours == 0.0) {
            Toast.makeText(
                requireContext(),
                "Пожалуйста, заполните все поля корректно",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        getSelectedCarId { carId ->
            if (carId == null) {
                Toast.makeText(requireContext(), "Не выбран автомобиль", Toast.LENGTH_SHORT).show()
                return@getSelectedCarId
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("cars/$carId/fuelPrice")
            dbRef.get().addOnSuccessListener { fuelPriceSnapshot ->
                val fuelPrice = fuelPriceSnapshot.getValue(Double::class.java) ?: 0.0

                val avgSpeed = distance / engineHours
                val fuelConsumption = (fuelUsed / distance) * 100
                val fuelCost = fuelUsed * fuelPrice

                // Создаем объект TripEntity
                val newTrip = TripEntity(
                    id = FirebaseDatabase.getInstance().reference.child("trips").push().key ?: "",
                    carId = carId,
                    date = selectedDate,
                    distance = distance,
                    fuelUsed = fuelUsed,
                    engineHours = engineHours,
                    avgSpeed = avgSpeed,
                    fuelConsumption = fuelConsumption,
                    fuelCost = fuelCost,
                    fuelPrice = fuelPrice
                )

                // Сохраняем поездку в Firebase
                val tripRef = FirebaseDatabase.getInstance()
                    .getReference("cars/$carId/trips/${newTrip.id}")

                tripRef.setValue(newTrip).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Поездка сохранена", Toast.LENGTH_SHORT).show()

                    // Обновляем общий пробег автомобиля
                    updateCarMileage(carId, distance.toInt())

                    // Обновляем пробег всех подходящих комплектующих
                    updatePartsAfterTripAdded(carId, distance.toInt())

                    findNavController().navigateUp()
                }.addOnFailureListener { e ->
                    Log.e("AddTripFragment", "Ошибка сохранения поездки", e)
                    Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e("AddTripFragment", "Ошибка получения fuelPrice", e)
                Toast.makeText(
                    requireContext(),
                    "Ошибка получения цены топлива",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Обновляет общий пробег автомобиля
     */
    private fun updateCarMileage(carId: String, tripDistance: Int) {
        val carRef =
            FirebaseDatabase.getInstance().getReference("cars").child(carId).child("mileage")

        carRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentMileage = mutableData.getValue(Long::class.java) ?: 0L
                mutableData.value = currentMileage + tripDistance
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (committed) {
                    Log.d("AddTripFragment", "Пробег авто успешно обновлён")
                } else {
                    Log.e("AddTripFragment", "Ошибка обновления пробега авто")
                }
            }
        })
    }

    /**
     * Обновляет пробег у всех комплектующих, которые были добавлены до этой поездки
     */
    private fun updatePartsAfterTripAdded(carId: String, tripDistance: Int) {
        val partsRef =
            FirebaseDatabase.getInstance().getReference("cars").child(carId).child("parts")

        partsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (partSnapshot in snapshot.children) {
                    val part = partSnapshot.getValue(CarPart::class.java) ?: continue

                    if (isTripAfterPartAdded(part.addedDate)) {
                        val updatedCurrentMileage = part.currentMileage + tripDistance
                        partsRef.child(part.id).child("currentMileage")
                            .setValue(updatedCurrentMileage)
                            .addOnFailureListener { e ->
                                Log.e(
                                    "AddTripFragment",
                                    "Ошибка обновления детали: ${part.name}",
                                    e
                                )
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AddTripFragment", "Ошибка при обновлении деталей: ${error.message}")
            }
        })
    }

    /**
     * Проверяет, была ли поездка совершена после добавления детали
     */
    private fun isTripAfterPartAdded(addedDateString: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val addedDate = sdf.parse(addedDateString) ?: return false
        val today = Date()

        return addedDate.before(today)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}