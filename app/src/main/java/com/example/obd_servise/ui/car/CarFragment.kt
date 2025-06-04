package com.example.obd_servise.ui.car

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentCarBinding
import com.example.obd_servise.databinding.ItemCarBinding
import kotlin.text.clear

class CarFragment : Fragment() {
    private var isInDeleteMode = false
    private val selectedCars = mutableSetOf<String>()


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
        carsAdapter = CarsAdapter(
            onItemClick = { car ->
                findNavController().navigate(
                    R.id.action_carFragment_to_carInputFragment,
                    Bundle().apply { putString("carId", car.id) }
                )
            },
            onSelectionChanged = { selected ->
                selectedCars.clear()
                selectedCars.addAll(selected)
            }
        )


        binding.linearLayoutCars.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carsAdapter
        }

        binding.fabDelCar.setOnClickListener {
            if (isInDeleteMode) {
                if (selectedCars.isNotEmpty()) {
                    // Показать подтверждение удаления
                    AlertDialog.Builder(requireContext())
                        .setTitle("Удаление автомобилей")
                        .setMessage("Вы точно хотите удалить выбранные автомобили?")
                        .setPositiveButton("Да") { _, _ ->
                            selectedCars.forEach { carId ->
                                viewModel.deleteCarFromFirebase(carId, {}, {})
                            }
                            exitDeleteMode()
                        }
                        .setNegativeButton("Нет") { _, _ -> exitDeleteMode() }
                        .show()
                } else {
                    exitDeleteMode()
                }
            } else {
                enterDeleteMode()
            }
        }

        binding.fabAddCar.setOnClickListener {
            if (!isInDeleteMode) {
                findNavController().navigate(
                    R.id.action_carFragment_to_carInputFragment,
                    Bundle().apply { putString("carId", "") }
                )
            } else {
                exitDeleteMode()
            }
        }


        // Наблюдаем за списком машин
        viewModel.carList.observe(viewLifecycleOwner) { cars ->
            carsAdapter.submitList(cars)
        }
    }

    private fun enterDeleteMode() {
        isInDeleteMode = true
        selectedCars.clear()

        // Получаем цвет из темы (аналог ?attr/colorPrimary)
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            androidx.appcompat.R.attr.colorPrimary,
            typedValue,
            true
        )
        val colorPrimary = typedValue.data

        // Меняем текст
        binding.fabDelCar.text = "Удалить"
        binding.fabAddCar.text = "Отмена"

        // fabDelCar → стиль btn1
        with(binding.fabDelCar) {
            setTextColor(resources.getColor(R.color.black, null))
            background = resources.getDrawable(R.drawable.btn1, null)
            backgroundTintList = ColorStateList.valueOf(colorPrimary)
        }

        // fabAddCar → стиль btn2_selected
        with(binding.fabAddCar) {
            setTextColor(resources.getColor(R.color.white, null))
            background = resources.getDrawable(R.drawable.btn2_selected, null)
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }

        // Меняем порядок кнопок
        binding.buttons.removeView(binding.fabDelCar)
        binding.buttons.removeView(binding.fabAddCar)
        binding.buttons.addView(binding.fabAddCar)
        binding.buttons.addView(binding.fabDelCar)

        carsAdapter.setDeleteMode(true)
    }

    private fun exitDeleteMode() {
        isInDeleteMode = false
        selectedCars.clear()

        binding.fabDelCar.text = "Удалить"
        binding.fabAddCar.text = "Добавить автомобиль"

        with(binding.fabDelCar) {
            setTextColor(resources.getColor(R.color.white, null))
            background = resources.getDrawable(R.drawable.btn2_selected, null)
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            androidx.appcompat.R.attr.colorPrimary,
            typedValue,
            true
        )
        val colorPrimary = typedValue.data

        with(binding.fabAddCar) {
            setTextColor(resources.getColor(R.color.black, null))
            background = resources.getDrawable(R.drawable.btn1, null)
            backgroundTintList = ColorStateList.valueOf(colorPrimary)
        }

        binding.buttons.removeView(binding.fabDelCar)
        binding.buttons.removeView(binding.fabAddCar)
        binding.buttons.addView(binding.fabDelCar)
        binding.buttons.addView(binding.fabAddCar)

        carsAdapter.setDeleteMode(false)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Адаптер для RecyclerView
    inner class CarsAdapter(
        private val onItemClick: (Car) -> Unit,
        private val onSelectionChanged: (Set<String>) -> Unit
    ) : ListAdapter<Car, CarsAdapter.CarViewHolder>(CarDiffCallback()) {

        private var deleteMode = false
        private val selectedCarIds = mutableSetOf<String>()

        fun setDeleteMode(enabled: Boolean) {
            deleteMode = enabled
            selectedCarIds.clear()
            notifyDataSetChanged()
        }

        inner class CarViewHolder(private val binding: ItemCarBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
            fun bind(car: Car) {
                binding.textViewCar.text = "${car.name} — ${car.fuelType}"

                if (deleteMode) {
                    binding.checkbox.visibility = View.VISIBLE
                    binding.checkbox.isChecked = selectedCarIds.contains(car.id)

                    binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedCarIds.add(car.id)
                        else selectedCarIds.remove(car.id)

                        onSelectionChanged(selectedCarIds)
                    }


                    binding.root.setOnClickListener {
                        binding.checkbox.isChecked = !binding.checkbox.isChecked
                    }
                } else {
                    binding.checkbox.visibility = View.GONE
                    binding.root.setOnClickListener {
                        onItemClick(car)
                    }
                }
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