package com.stillloading.mdschedule.taskutils

import android.content.Context
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.data.TimeTaskDisplayData
import com.stillloading.mdschedule.databinding.ItemTimeTaskBinding
import java.time.LocalTime
import kotlin.math.min

class TimeTaskManager (
    private val context: Context,
    private val frameContainer: FrameLayout,
    private val frameLayout: FrameLayout,
    private val layoutInflater: LayoutInflater,
    private val popup: TaskPopup
){

    var timeBarView: ImageView? = null

    private val defaultTime: Float = 0.5f // minutes fraction
    private val hourHeight: Int = context.resources.getDimension(R.dimen.hour_height).toInt()
    private val hourWidth: Int = 750
    private val maxWidthDivision: Int = 2 // changing this number breaks some stuff in complex layouts
    private val defaultTaskMarginStart: Int = context.resources.getDimension(R.dimen.time_task_margin_start).toInt()

    private val hourRegEx = Regex("^(?<hour>\\d\\d?):(?<minutes>\\d{2})?")

    private fun resetTasks(){
        frameLayout.removeAllViews()
    }

    fun setTimeTasks(tasks: MutableList<TaskDisplayData>, minHour: Int, maxHour: Int){
        resetTasks()

        val taskDisplayList = getVerticalVariables(tasks, minHour)
        setHorizontalVariables(taskDisplayList)

        for(taskDisplay in taskDisplayList){
            val itemTimeTaskBinding = ItemTimeTaskBinding.inflate(layoutInflater)
            setItemTaskProperties(tasks[taskDisplay.id], itemTimeTaskBinding)


            val params = FrameLayout.LayoutParams(
                taskDisplay.width,
                taskDisplay.height
            ).apply {
                //marginStart = taskDisplay.x + defaultTaskMarginStart
                leftMargin = taskDisplay.x + defaultTaskMarginStart
                topMargin = taskDisplay.y
            }

            frameLayout.addView(itemTimeTaskBinding.root, params)
        }

        setTimeBar(minHour, maxHour)
    }


    fun setTimeBar(minHour: Int, maxHour: Int){
        if(timeBarView != null){
            frameContainer.removeView(timeBarView)
        }

        val timeNow = LocalTime.now()

        if(timeNow.hour in minHour..<maxHour){
            timeBarView = ImageView(context).apply {
                setImageResource(R.drawable.time_indicator_bar)
            }

            var y = (timeNow.hour - minHour) * hourHeight
            val minutesAdded: Float = (timeNow.minute / 60f) * hourHeight
            y += minutesAdded.toInt()

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = context.resources.getDimension(R.dimen.hour_number_width).toInt()
                topMargin = y
            }

            frameContainer.addView(timeBarView, params)
        }
    }


    private fun setItemTaskProperties(currTask: TaskDisplayData, itemTask: ItemTimeTaskBinding){
        itemTask.apply {
            tvTask.text = currTask.task
            toggleStrikeThrough(rlTimeTaskList, tvTask, currTask.isChecked)

            root.setOnClickListener{
                popup.show(currTask, root)
            }
        }
    }

    private fun getVerticalVariables(tasks: MutableList<TaskDisplayData>, minHour: Int): MutableList<TimeTaskDisplayData>{
        val taskDisplayList: MutableList<TimeTaskDisplayData> = mutableListOf()

        for (id in 0..< tasks.size){
            val y = getY(minHour, tasks[id].evStartTime)
            if(y == -1) continue

            val height = getHeight(tasks[id].evStartTime, tasks[id].evEndTime)
            if(height == -1) continue

            taskDisplayList.add(
                TimeTaskDisplayData(
                    id = id,
                    y = y,
                    height = height,
                )
            )
        }

        return taskDisplayList
    }

    private fun getY(minHour: Int, startTimeString: String?) : Int{
        if(startTimeString == null) return -1
        var y = 0

        val (hour, minutes) = getHourMinutes(startTimeString)

        if(hour != null){
            y = (hour - minHour) * hourHeight
            val minutesAdded: Float = minutes * hourHeight
            y += minutesAdded.toInt()
        }


        return y
    }

    private fun getHeight(startTimeString: String?, endTimeString: String?): Int{
        var height = (hourHeight * defaultTime).toInt()

        val (startHour, startMinutes) = getHourMinutes(startTimeString)
        if(startHour == null) return -1

        val (endHour, endMinutes) = getHourMinutes(endTimeString)
        if(endHour == null) return height

        height = (((endHour + endMinutes) - (startHour + startMinutes)) * hourHeight).toInt()

        return height
    }

    private fun setHorizontalVariables(tasks: MutableList<TimeTaskDisplayData>){
        for(i in 0..<tasks.size){
            // get the overlapping order of the current task
            while(tasks[i].overlappingTasks.contains(tasks[i].overlappingOrder)){
                tasks[i].overlappingOrder++
            }

            for(j in i + 1..<tasks.size){
                if(tasksOverlap(tasks[i], tasks[j])){
                    // get the overlapping order for the tasks that the current task is checking
                    tasks[j].overlappingTasks.add(tasks[i].overlappingOrder)
                    while(tasks[j].overlappingTasks.contains(tasks[j].overlappingOrder)){
                        tasks[j].overlappingOrder++
                    }

                    // add the checked task so that you can tell if you should shrink
                    tasks[i].numOverlappingTasks++
                    tasks[j].numOverlappingTasks++
                }
            }


            tasks[i].width = hourWidth / min(tasks[i].numOverlappingTasks, maxWidthDivision)
            tasks[i].x = tasks[i].overlappingOrder * tasks[i].width
        }
    }

    private fun tasksOverlap(task1: TimeTaskDisplayData, task2: TimeTaskDisplayData): Boolean{
        val start1 = task1.y
        val end1 = task1.y + task1.height
        val start2 = task2.y
        val end2 = task2.y + task2.height

        return (start1 < end2 && start2 < end1) or (start2 < end1 && start1 < end2) or
                (start1 < start2 && end2 < end1) or (start2 < start1 && end1 < end2)
    }

    private fun getHourMinutes(timeString: String?): Pair<Int?, Float>{
        if (timeString == null) return Pair(null, 0f)

        val match = hourRegEx.find(timeString)?.groups
        val hourString = match?.get("hour")?.value
        val minutesString = match?.get("minutes")?.value

        var hour: Int? = null
        var minutes: Float = 0f

        if(hourString!= null){
            hour = hourString.toInt()
            if(minutesString != null){
                minutes = minutesString.toFloat() / 60
            }
        }

        return Pair(hour, minutes)
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