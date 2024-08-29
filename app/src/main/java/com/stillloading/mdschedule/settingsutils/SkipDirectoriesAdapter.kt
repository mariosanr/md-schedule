package com.stillloading.mdschedule.settingsutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
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