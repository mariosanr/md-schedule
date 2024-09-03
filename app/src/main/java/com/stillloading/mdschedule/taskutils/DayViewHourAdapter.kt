/*
    Markdown Schedule: Android schedule from Markdown files
    Copyright (C) 2024  Mario San Roman Caraza

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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