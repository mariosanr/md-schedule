package com.stillloading.mdschedule.taskutils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.databinding.PopupTaskBinding


class TaskPopup(private val popupBinding: PopupTaskBinding, private val context: Context) {

    private val width = ViewGroup.LayoutParams.MATCH_PARENT
    private val height = ViewGroup.LayoutParams.MATCH_PARENT
    private var maxHeight = 1900
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
                val evTimeString = if(task.evEndTime != null)
                    "${task.evStartTime} - ${task.evEndTime}" else task.evStartTime.toString()
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

            // FIXME necesito obtener el height de toda la aplicación, así no funciona bien
            // set the max height for the popup box
            scrollView.post{
                //maxHeight = (root.height * 0.9).toInt()
                //Toast.makeText(context, "The new max height is $maxHeight", Toast.LENGTH_LONG).show()
                scrollView.layoutParams.height = if(scrollView.height > maxHeight) maxHeight else scrollView.height
                mainLayout.requestLayout()
            }
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