package com.example.obd_servise.ui.car

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarPartsBinding
import com.example.obd_servise.databinding.ItemCarPartBinding

// Define CarPart data class at the top level of the file
data class CarPart(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val lastReplacementDate: String = "",
    val replacementInterval: Int = 0,
    val nextReplacementDate: String = "",
    val price: Double = 0.0
)

class CarPartsFragment : Fragment() {

    private var _binding: FragmentCarPartsBinding? = null
    private val binding get() = _binding!!
    private lateinit var carPartsAdapter: CarPartsAdapter
    private var selectedCarId: String? = null
    private val allParts = mutableListOf<CarPart>() // All parts
    private val displayedParts = mutableListOf<CarPart>() // Displayed parts

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize test data
        initTestData()

        // Initialize RecyclerView
        carPartsAdapter = CarPartsAdapter(displayedParts)
        binding.carPartsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carPartsAdapter
        }

        // Update displayed parts (all initially)
        updateDisplayedParts(allParts)

        // Setup search
        binding.searchPartsEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterParts(s?.toString() ?: "")
            }
        })

        // Add part button
        binding.addPartButton.setOnClickListener {
            // Add new part implementation
        }

        // Change view button
        binding.changeViewButton.setOnClickListener {
            // Change view implementation
        }
    }

    private fun initTestData() {
        // Clear any existing data
        allParts.clear()

        // Add 20 test parts
        allParts.addAll(
            listOf(
                CarPart(
                    "1",
                    "Oil Filter",
                    "Original oil filter",
                    "2023-01-15",
                    15000,
                    "2023-07-15",
                    25.99
                ),
                CarPart(
                    "2",
                    "Air Filter",
                    "Standard air filter",
                    "2023-02-20",
                    30000,
                    "2024-02-20",
                    15.50
                ),
                CarPart(
                    "3",
                    "Cabin Filter",
                    "Carbon cabin filter",
                    "2023-03-10",
                    20000,
                    "2023-09-10",
                    18.75
                ),
                CarPart(
                    "4",
                    "Brake Pads",
                    "Front Brembo pads",
                    "2022-11-05",
                    50000,
                    "2024-11-05",
                    65.30
                ),
                CarPart(
                    "5",
                    "Brake Discs",
                    "Vented front discs",
                    "2022-11-05",
                    80000,
                    "2025-11-05",
                    120.00
                ),
                CarPart(
                    "6",
                    "Spark Plugs",
                    "NGK Platinum",
                    "2023-04-01",
                    40000,
                    "2025-04-01",
                    12.99
                ),
                CarPart(
                    "7",
                    "Timing Belt",
                    "Gates belt with tensioner",
                    "2021-09-15",
                    100000,
                    "2026-09-15",
                    85.40
                ),
                CarPart(
                    "8",
                    "Battery",
                    "Varta Blue Dynamic",
                    "2023-05-20",
                    0,
                    "2026-05-20",
                    150.00
                ),
                CarPart(
                    "9",
                    "Summer Tires",
                    "Michelin Pilot Sport 4",
                    "2023-04-15",
                    50000,
                    "2025-04-15",
                    200.00
                ),
                CarPart(
                    "10",
                    "Winter Tires",
                    "Nokian Hakkapeliitta R5",
                    "2022-11-01",
                    50000,
                    "2024-11-01",
                    180.00
                ),
                CarPart("11", "Engine Oil", "Castrol Edge 5W-30", "", 15000, "", 45.00),
                CarPart(
                    "12",
                    "Transmission Oil",
                    "Liqui Moly 75W-90",
                    "2020-07-10",
                    60000,
                    "2025-07-10",
                    35.20
                ),
                CarPart(
                    "13",
                    "Coolant",
                    "Felix Prolong",
                    "2022-08-12",
                    100000,
                    "2027-08-12",
                    22.50
                ),
                CarPart("14", "Brake Fluid", "DOT 4", "2023-01-30", 2, "2025-01-30", 10.80),
                CarPart(
                    "15",
                    "Wiper Blades",
                    "Bosch Aerotwin",
                    "2023-06-01",
                    1,
                    "2024-06-01",
                    25.00
                ),
                CarPart("16", "Low Beam Bulb", "Philips X-tremeVision", "2023-03-15", 0, "", 30.00),
                CarPart("17", "High Beam Bulb", "Osram Night Breaker", "2023-03-15", 0, "", 32.00),
                CarPart(
                    "18",
                    "Front Shocks",
                    "KYB Excel-G",
                    "2021-10-20",
                    80000,
                    "2025-10-20",
                    95.00
                ),
                CarPart(
                    "19",
                    "Bushings",
                    "Lemforder OEM",
                    "2022-05-10",
                    100000,
                    "2027-05-10",
                    45.50
                ),
                CarPart(
                    "20",
                    "Steering Rack",
                    "Repair kit",
                    "2023-02-28",
                    50000,
                    "2026-02-28",
                    75.25
                )
            )
        )
    }

    private fun filterParts(query: String) {
        if (query.isEmpty()) {
            updateDisplayedParts(allParts)
            return
        }

        val filtered = allParts.filter { part ->
            part.name.contains(query, true) ||
                    part.description.contains(query, true) ||
                    part.lastReplacementDate.contains(query, true) ||
                    part.nextReplacementDate.contains(query, true) ||
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
                partDescriptionTextView.text = part.description
                partReplacementInfoTextView.text =
                    "Last replaced: ${part.lastReplacementDate} • Next replacement: ${part.nextReplacementDate}"
                partPriceTextView.text = "Price: ${part.price} €"
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

    override fun getItemCount() = parts.size
}