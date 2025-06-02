//package com.example.obd_servise.ui.home
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.example.obd_servise.ui.car.CarPart
//
//class ComponentsAdapter(private val parts: List<CarPart>) :
//    RecyclerView.Adapter<ComponentsAdapter.ComponentViewHolder>() {
//
//    inner class ComponentViewHolder(private val binding: ItemComponentBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(part: CarPart) {
//            with(binding) {
//                componentName.text = part.name
//                componentStatus.text = when (part.condition) {
//                    "normal" -> "OK"
//                    "warning" -> "WARNING"
//                    "critical" -> "CRITICAL"
//                    else -> "UNKNOWN"
//                }
//
//                // Установка цвета фона в зависимости от состояния
//                val bgColor = when (part.condition) {
//                    "normal" -> ContextCompat.getColor(itemView.context, R.color.green_light)
//                    "warning" -> ContextCompat.getColor(itemView.context, R.color.orange_light)
//                    "critical" -> ContextCompat.getColor(itemView.context, R.color.red_light)
//                    else -> ContextCompat.getColor(itemView.context, android.R.color.white)
//                }
//
//                root.setBackgroundColor(bgColor)
//
//                // Дополнительная информация при клике
//                root.setOnClickListener {
//                    Toast.makeText(
//                        itemView.context,
//                        "Пробег: ${part.currentMileage} км / ${part.recommendedMileage} км",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder {
//        val binding = ItemComponentBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return ComponentViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) {
//        holder.bind(parts[position])
//    }
//
//    override fun getItemCount(): Int = parts.size
//}