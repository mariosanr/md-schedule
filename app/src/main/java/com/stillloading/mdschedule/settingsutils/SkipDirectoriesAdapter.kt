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
import com.stillloading.mdschedule.data.SkipDirectoryData
import com.stillloading.mdschedule.databinding.ItemSkipDirectoryBinding

class SkipDirectoriesAdapter (
    var skipDirectoryList: MutableList<SkipDirectoryData>
) : RecyclerView.Adapter<SkipDirectoriesAdapter.SkipDirectoryViewHolder>() {

    private lateinit var itemSkipDirectoryBinding: ItemSkipDirectoryBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkipDirectoryViewHolder {
        itemSkipDirectoryBinding = ItemSkipDirectoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SkipDirectoryViewHolder(itemSkipDirectoryBinding)
    }

    fun addItem(directory: SkipDirectoryData){
        skipDirectoryList.add(directory)
        notifyItemInserted(skipDirectoryList.size - 1)
    }

    fun deleteItem(directory: SkipDirectoryData){
        skipDirectoryList.removeAt(directory.position)
        for(i in directory.position..<skipDirectoryList.size){
            skipDirectoryList[i].position = i
        }

        notifyItemRemoved(directory.position)
    }


    override fun onBindViewHolder(holder: SkipDirectoryViewHolder, position: Int) {
        skipDirectoryList[position].position = position
        val currDirectory = skipDirectoryList[position]
        holder.bind(currDirectory, this)
    }

    override fun getItemCount(): Int {
        return skipDirectoryList.size
    }

    class SkipDirectoryViewHolder(private val binding: ItemSkipDirectoryBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(currDirectory: SkipDirectoryData, rvAdapter: SkipDirectoriesAdapter){
            binding.apply {
                etSkipDirectory.setText(currDirectory.text)
                currDirectory.editableText = etSkipDirectory.text
                /*
                etSkipDirectory.doAfterTextChanged { text ->
                    currDirectory.text = text.toString()
                }
                 */
                ibRemoveSkipDirectory.setOnClickListener {
                    rvAdapter.deleteItem(currDirectory)
                }
            }
        }
    }

}