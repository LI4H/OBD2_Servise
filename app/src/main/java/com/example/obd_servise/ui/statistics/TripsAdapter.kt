package com.example.obd_servise.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_servise.databinding.ItemTripBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripsAdapter(
    private var trips: List<TripEntity>,
    private val onItemClick: (TripEntity) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.binding.apply {
            tvDate.text = formatDate(trip.date)
            tvDistance.text = "Дистанция: ${"%.1f".format(trip.distance)} км"
            tvFuelUsed.text = "Топливо: ${"%.1f".format(trip.fuelUsed)} л"
            tvFuelCost.text = "Стоимость: ${"%.1f".format(trip.fuelCost)} ₽"

            root.setOnClickListener { onItemClick(trip) }
        }
    }

    override fun getItemCount() = trips.size

    fun updateData(newTrips: List<TripEntity>) {
        trips = newTrips.sortedByDescending { it.date }
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}