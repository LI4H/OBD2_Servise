package com.example.obd_servise.ui.car

import NotificationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_servise.databinding.FragmentCarPartsBinding
import com.example.obd_servise.databinding.ItemCarPartBinding
import com.example.obd_servise.ui.settings.SettingsViewModel
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

data class CarPart(
    val id: String = "",
    val name: String = "",
    val recommendedMileage: Int = 0,
    val currentMileage: Int = 0,
    val addedDate: String = "",
    val price: Double = 0.0,
    val notificationsEnabled: Boolean = false
) {
    val condition: String
        get() {
            if (recommendedMileage == 0) return "normal"
            val percentage = (currentMileage.toDouble() / recommendedMileage) * 100
            return when {
                percentage < 75 -> "normal"
                percentage <= 100 -> "warning"
                else -> "critical"
            }
        }
}

class CarPartsFragment : Fragment() {
    private lateinit var notificationManager: NotificationManager
    private var _binding: FragmentCarPartsBinding? = null
    private val binding get() = _binding!!
    private lateinit var carPartsAdapter: CarPartsAdapter
    private val allParts = mutableListOf<CarPart>()
    private val displayedParts = mutableListOf<CarPart>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var selectedCarId: String? = null
    private lateinit var carViewModel: CarViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        carViewModel = ViewModelProvider(requireActivity()).get(CarViewModel::class.java)
        settingsViewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)



        _binding = FragmentCarPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationManager = NotificationManager(requireContext(), carViewModel, settingsViewModel)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        selectedCarId = arguments?.getString("carId")

        carPartsAdapter = CarPartsAdapter(displayedParts)
        binding.carPartsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carPartsAdapter
        }

        binding.searchPartsEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterParts(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.addPartButton.setOnClickListener {
            val action = CarPartsFragmentDirections.actionCarPartsFragmentToAddCarPartFragment(
                carId = selectedCarId ?: ""
            )
            findNavController().navigate(action)
        }

        fetchCarParts()
    }

    private fun fetchCarParts() {
        val carsRef = FirebaseDatabase.getInstance().getReference("cars")
        carsRef.orderByChild("isSelected").equalTo(1.0).get()
            .addOnSuccessListener { snapshot ->
                val selectedCarSnapshot = snapshot.children.firstOrNull() ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Не найден выбранный автомобиль",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val carId = selectedCarSnapshot.key ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Не удалось получить ID автомобиля",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                val carName = selectedCarSnapshot.child("name").getValue(String::class.java) ?: ""
                val partsRef =
                    FirebaseDatabase.getInstance().getReference("cars").child(carId).child("parts")
                partsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val parts =
                            snapshot.children.mapNotNull { it.getValue(CarPart::class.java) }
                        updateDisplayedParts(parts)
                        notificationManager.checkAndSendNotifications(parts, carId, carName)
                        allParts.clear()
                        for (partSnapshot in snapshot.children) {
                            val part = partSnapshot.getValue(CarPart::class.java)
                            if (part != null) {
                                allParts.add(part)
                            } else {
                                Log.e(
                                    "CarPartsFragment",
                                    "Ошибка парсинга детали: ${partSnapshot.key}"
                                )
                            }
                        }
                        updateDisplayedParts(allParts)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка загрузки деталей",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
            .addOnFailureListener { error ->
                Log.e("CarPartsFragment", "Ошибка получения выбранного авто", error)
                Toast.makeText(requireContext(), "Ошибка поиска авто", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterParts(query: String) {
        if (query.isEmpty()) {
            updateDisplayedParts(allParts)
            return
        }

        val filtered = allParts.filter { part ->
            part.name.contains(query, true) ||
                    part.condition.contains(query, true) ||
                    part.addedDate.contains(query, true) ||
                    part.price.toString().contains(query, true)
        }

        updateDisplayedParts(filtered)
    }

    private fun updateDisplayedParts(parts: List<CarPart>) {
        displayedParts.clear()
        displayedParts.addAll(parts)
        carPartsAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(carId: String): CarPartsFragment {
            return CarPartsFragment().apply {
                arguments = Bundle().apply {
                    putString("carId", carId)
                }
            }
        }
    }
}

class CarPartsAdapter(private val parts: List<CarPart>) :
    RecyclerView.Adapter<CarPartsAdapter.CarPartViewHolder>() {

    inner class CarPartViewHolder(private val binding: ItemCarPartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(part: CarPart) {
            with(binding) {
                partNameTextView.text = part.name
                partConditionTextView.text = "Состояние: ${part.condition}"
                partMileageTextView.text = "Текущий пробег: ${part.currentMileage} км"
                partRecommendedMileageTextView.text =
                    "Рекомендуемый пробег: ${part.recommendedMileage} км"
                partPriceTextView.text = "Стоимость: ${part.price} бел. руб."

                // Установка цвета в зависимости от состояния
                val backgroundRes = when (part.condition) {
                    "normal" -> android.R.color.holo_green_light
                    "warning" -> android.R.color.holo_orange_light
                    "critical" -> android.R.color.holo_red_light
                    else -> android.R.color.white
                }

                partContainer.setBackgroundResource(backgroundRes)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarPartViewHolder {
        val binding = ItemCarPartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CarPartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarPartViewHolder, position: Int) {
        holder.bind(parts[position])
    }

    override fun getItemCount(): Int = parts.size
}