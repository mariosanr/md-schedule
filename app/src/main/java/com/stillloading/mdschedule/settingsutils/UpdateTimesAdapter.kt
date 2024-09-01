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