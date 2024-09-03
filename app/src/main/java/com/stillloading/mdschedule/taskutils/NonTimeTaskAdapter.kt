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
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.databinding.ItemTaskBinding

class NonTimeTaskAdapter(
    private var taskList: MutableList<TaskDisplayData>,
    private val popup: TaskPopup,
    private var context: Context
) : RecyclerView.Adapter<NonTimeTaskViewHolder>() {

    init {
        sortTaskList(taskList)
    }

    private fun sortTaskList(list: MutableList<TaskDisplayData>){
        list.sortWith(compareBy({it.isChecked}, {-it.priorityNumber}))
    }


    @SuppressLint("NotifyDataSetChanged")
    fun reloadTasks(newTaskList: MutableList<TaskDisplayData>){
        sortTaskList(newTaskList)
        taskList = newTaskList
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NonTimeTaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NonTimeTaskViewHolder(binding, popup, context)
    }

    override fun onBindViewHolder(holder: NonTimeTaskViewHolder, position: Int) {
        val currTask = taskList[position]
        holder.bind(currTask)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

}