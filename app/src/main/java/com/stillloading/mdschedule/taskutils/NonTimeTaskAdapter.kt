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