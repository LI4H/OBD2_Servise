package com.example.obd_servise.ui.car

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarBinding

class CarFragment : Fragment() {

    private var _binding: FragmentCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Кнопка "добавить машину"
        binding.fabAddCar.setOnClickListener {
            findNavController().navigate(
                R.id.action_carFragment_to_carInputFragment,
                Bundle().apply {
                    putString("carId", "")
                }
            )
        }

        // Список машин
        viewModel.carList.observe(viewLifecycleOwner) { cars ->
            binding.linearLayoutCars.removeAllViews()
            for (car in cars) {
                val carView = layoutInflater.inflate(
                    R.layout.item_car, binding.linearLayoutCars, false
                ) as TextView
                carView.text = "${car.name} — ${car.fuelType}"

                carView.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("carId", car.id)
                    }
                    findNavController().navigate(
                        R.id.action_carFragment_to_carInputFragment, bundle
                    )
                }

                binding.linearLayoutCars.addView(carView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
