package com.stillloading.mdschedule.taskutils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.databinding.PopupTaskBinding


class TaskPopup(private val popupBinding: PopupTaskBinding, private val context: Context) {

    private val width = ViewGroup.LayoutParams.MATCH_PARENT
    private val height = ViewGroup.LayoutParams.MATCH_PARENT
    private val focusable = true

    fun show(task: TaskDisplayData, view: View){
        popupBinding.apply {
            tvTaskDateSummary.text = task.taskSummary
            if(task.priority != ""){
                val priorityString = "${task.prioritySymbol} ${task.priority}"
                tvPriority.text = priorityString
                tvPriority.visibility = View.VISIBLE
            }else{
                tvPriority.visibility = View.GONE
            }
            tvStatus.text = task.status

            val scheduledDateString = "Scheduled: ${task.scheduledDate}"
            val startDateString = "Start: ${task.startDate}"
            val dueDateString = "Due: ${task.dueDate}"
            tvScheduledDate.text = scheduledDateString
            tvStartDate.text = startDateString
            tvDueDate.text = dueDateString

            if(task.evDate != "N/A"){
                val evDateString = "Event: ${task.evDate}"
                val evTimeString = if(task.evStartTime != null && task.evEndTime != null)
                    "${task.evStartTime} - ${task.evEndTime}" else task.evStartTime ?: ""
                tvEvDate.text = evDateString
                tvEvTime.text = evTimeString
                tvEvDateTime.text = task.evDateTimeString
                tvEvDate.visibility = View.VISIBLE
                tvEvTime.visibility = View.VISIBLE
                tvEvDateTime.visibility = View.VISIBLE
            }else{
                tvEvDate.visibility = View.GONE
                tvEvTime.visibility = View.GONE
                tvEvDateTime.visibility = View.GONE
            }

            tvTask.text = task.task
        }

        val popup = PopupWindow(popupBinding.root, width, height, focusable)

        popupBinding.root.setOnClickListener{
            popup.dismiss()
        }

        popupBinding.ibClose.setOnClickListener{
            popup.dismiss()
        }

        popup.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

}