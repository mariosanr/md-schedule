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
