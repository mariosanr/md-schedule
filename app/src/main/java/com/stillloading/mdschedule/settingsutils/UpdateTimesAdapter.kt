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

package com.stillloading.mdschedule.settingsutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.UpdateTimesData
import com.stillloading.mdschedule.databinding.ItemUpdateTimeBinding

class UpdateTimesAdapter(
    var updateTimesList: MutableList<UpdateTimesData>
) : RecyclerView.Adapter<UpdateTimesAdapter.UpdateTimesViewHolder>(){

    private lateinit var itemUpdateTimeBinding: ItemUpdateTimeBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateTimesViewHolder {
        itemUpdateTimeBinding = ItemUpdateTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return UpdateTimesViewHolder(itemUpdateTimeBinding)
    }

    fun addItem(updateTime: UpdateTimesData){
        updateTimesList.add(updateTime)
        notifyItemInserted(updateTimesList.size - 1)
    }

    fun deleteItem(updateTime: UpdateTimesData){
        updateTimesList.removeAt(updateTime.position)
        for(i in updateTime.position..<updateTimesList.size){
            updateTimesList[i].position = i
        }

        notifyItemRemoved(updateTime.position)
    }

    override fun onBindViewHolder(holder: UpdateTimesViewHolder, position: Int) {
        updateTimesList[position].position = position
        val currTime = updateTimesList[position]
        holder.bind(currTime, this)
    }


    override fun getItemCount(): Int {
        return updateTimesList.size
    }



    class UpdateTimesViewHolder(private val binding: ItemUpdateTimeBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(currTime: UpdateTimesData, rvAdapter: UpdateTimesAdapter){
            binding.apply {
                tvUpdateTime.text = currTime.timeString
                ibRemoveTime.setOnClickListener {
                    rvAdapter.deleteItem(currTime)
                }
            }
        }
    }

}