package com.example.obd_servise.ui.statistics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.R
import com.example.obd_servise.ui.statistics.TripsAdapter
import com.example.obd_servise.databinding.FragmentAllTripsBinding
import com.example.obd_servise.ui.car.CarViewModel
import dagger.hilt.android.AndroidEntryPoint

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class AllTripsFragment : Fragment(R.layout.fragment_all_trips) {

    private val statisticsViewModel: StatisticsViewModel by activityViewModels()
    private val carViewModel: CarViewModel by activityViewModels()
    private var _binding: FragmentAllTripsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAllTripsBinding.bind(view)

        setupRecyclerView()
        observeTrips()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = TripsAdapter(emptyList()) { trip ->
                // Обработка клика на поездку (если нужно)
            }
        }
    }

    private fun observeTrips() {
        carViewModel.getSelectedCar { selectedCar ->
            selectedCar?.let { car ->
                statisticsViewModel.getTripsForCar(car.id)
                statisticsViewModel.trips.observe(viewLifecycleOwner) { trips ->
                    if (trips.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        (binding.recyclerView.adapter as TripsAdapter).updateData(trips)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}