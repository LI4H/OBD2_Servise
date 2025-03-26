package com.example.obd_servise.ui.errors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_servise.R

class ErrorsAdapter(private val errors: MutableList<ErrorItem>) : RecyclerView.Adapter<ErrorsAdapter.ErrorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_error, parent, false)
        return ErrorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ErrorViewHolder, position: Int) {
        val error = errors[position]
        holder.bind(error)
    }

    override fun getItemCount(): Int = errors.size

    fun updateErrors(newErrors: List<ErrorItem>) {
        errors.clear()
        errors.addAll(newErrors)
        notifyDataSetChanged()
    }

    inner class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val errorCode: TextView = itemView.findViewById(R.id.errorCode)
        private val errorStatus: TextView = itemView.findViewById(R.id.errorStatus)
        private val errorDescription: TextView = itemView.findViewById(R.id.errorDescription)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteErrorBtn)

        fun bind(error: ErrorItem) {
            errorCode.text = error.code
            errorStatus.text = error.status
            errorDescription.text = error.description

            deleteButton.visibility = View.GONE

            itemView.setOnLongClickListener {
                deleteButton.visibility = View.VISIBLE
                true
            }

            deleteButton.setOnClickListener {
                errors.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
            }
        }
    }
}
