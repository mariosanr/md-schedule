package com.stillloading.mdschedule.settingsutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.databinding.ItemDirectoryBinding

class DirectoryAdapter(
    var directoryList: MutableList<DirectoryData>
) : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    private lateinit var itemDirectoryBinding: ItemDirectoryBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        itemDirectoryBinding = ItemDirectoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DirectoryViewHolder(itemDirectoryBinding)
    }

    fun addItem(directory: DirectoryData){
        directoryList.add(directory)
        notifyItemInserted(directoryList.size - 1)
    }

    fun deleteItem(directory: DirectoryData){
        directoryList.removeAt(directory.position)
        for(i in directory.position..<directoryList.size){
            directoryList[i].position = i
        }

        notifyItemRemoved(directory.position)
    }


    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        directoryList[position].position = position
        val currDirectory = directoryList[position]
        holder.bind(currDirectory, this)
    }

    override fun getItemCount(): Int {
        return directoryList.size
    }


    class DirectoryViewHolder(private val binding: ItemDirectoryBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(currDirectory: DirectoryData, rvAdapter: DirectoryAdapter){
            binding.apply {
                tvDirectory.text = currDirectory.text
                ibRemoveDirectory.setOnClickListener {
                    rvAdapter.deleteItem(currDirectory)
                }
            }
        }
    }
}
