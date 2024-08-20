package com.stillloading.mdschedule.taskutils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.databinding.DayViewHourBinding

class DayViewHourAdapter(private var minHour: Int, private var maxHour: Int = 24)
    : RecyclerView.Adapter<DayViewHourAdapter.DayViewHourViewHolder>() {


    @SuppressLint("NotifyDataSetChanged")
    fun changeHours(newMinHour: Int, newMaxHour: Int){
        minHour = newMinHour
        maxHour = newMaxHour
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHourViewHolder {
        val binding = DayViewHourBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DayViewHourViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return maxHour - minHour
    }

    override fun onBindViewHolder(holder: DayViewHourViewHolder, position: Int) {
        val currHour = minHour + position
        holder.bind(currHour)
    }

    class DayViewHourViewHolder(private val binding: DayViewHourBinding)
        : RecyclerView.ViewHolder(binding.root){

        fun bind(currHour: Int){
            binding.apply {
                val hourString = currHour.toString()
                tvHour.text = hourString
            }
        }
    }

}