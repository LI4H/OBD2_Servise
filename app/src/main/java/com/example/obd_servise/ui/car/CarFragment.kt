package com.example.obd_servise.ui.car

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarBinding
import com.example.obd_servise.databinding.ItemCarBinding

class CarFragment : Fragment() {

    private var _binding: FragmentCarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarViewModel by viewModels()
    private lateinit var carsAdapter: CarsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Инициализация RecyclerView
        carsAdapter = CarsAdapter { car ->
            findNavController().navigate(
                R.id.action_carFragment_to_carInputFragment,
                Bundle().apply { putString("carId", car.id) }
            )
        }

        binding.linearLayoutCars.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carsAdapter
        }

        // Кнопка "добавить машину"
        binding.fabAddCar.setOnClickListener {
            findNavController().navigate(
                R.id.action_carFragment_to_carInputFragment,
                Bundle().apply { putString("carId", "") }
            )
        }

        // Наблюдаем за списком машин
        viewModel.carList.observe(viewLifecycleOwner) { cars ->
            carsAdapter.submitList(cars)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Адаптер для RecyclerView
    inner class CarsAdapter(
        private val onItemClick: (Car) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Car, CarsAdapter.CarViewHolder>(
        CarDiffCallback()
    ) {
        inner class CarViewHolder(private val binding: ItemCarBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(car: Car) {
                binding.textViewCar.text = "${car.name} — ${car.fuelType}"
                binding.root.setOnClickListener { onItemClick(car) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
            val binding = ItemCarBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CarViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    // Callback для сравнения элементов
    class CarDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Car, newItem: Car) = oldItem == newItem
    }
}