package com.stillloading.mdschedule.taskutils

import android.content.Context
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.databinding.ItemTaskBinding

class NonTimeTaskViewHolder(private val binding: ItemTaskBinding, private val popup: TaskPopup, private val context: Context)
    : RecyclerView.ViewHolder(binding.root){

    fun bind(currTask: TaskDisplayData){
        binding.apply {
            tvTask.text = currTask.task

            val summaryBuilder = StringBuilder("")
            if(currTask.status != "To Do" && currTask.status != "")
                summaryBuilder.append("${currTask.status}: ")
            summaryBuilder.append(
                if(currTask.evDate == "N/A")
                currTask.taskSummary else currTask.evDateTimeString
            )
            tvTaskDateSummary.text = summaryBuilder.toString()

            tvPriority.text = currTask.prioritySymbol
            toggleStrikeThrough(rlTaskList, tvTask, currTask.isChecked)
            root.setOnClickListener{
                popup.show(currTask, root)
            }
        }
    }

    private fun toggleStrikeThrough(rlTaskList: RelativeLayout, tvTask: TextView, isChecked: Boolean){
        if(isChecked){
            // Checked card
            tvTask.paintFlags = tvTask.paintFlags or STRIKE_THRU_TEXT_FLAG
            // TODO cambiar esto a una referencia de color en vez del color hard coded
            tvTask.setTextColor(context.getColor(R.color.checked_text))
            rlTaskList.setBackgroundResource(R.drawable.task_background_checked)
            //rlTaskList.setBackgroundColor(context.getColor(R.color.checked_card))
        }else{
            // Unchecked card
            tvTask.paintFlags = tvTask.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
            tvTask.setTextColor(context.getColor(R.color.unchecked_text))
            rlTaskList.setBackgroundResource(R.drawable.task_background)
            //rlTaskList.setBackgroundColor(context.getColor(R.color.unchecked_card))
        }
    }

}
