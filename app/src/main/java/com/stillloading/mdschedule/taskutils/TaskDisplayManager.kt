package com.stillloading.mdschedule.taskutils

import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.data.TaskPriority
import com.stillloading.mdschedule.data.TaskWidgetDisplayData
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskDisplayManager(private val settings: SettingsData) {

    private val parseTextRegEx = Regex("^\\s*(?:- \\[.]|-)\\s+(?:\\d{2}:\\d{2}\\s*-\\s*\\d{2}:\\d{2})?(?<text>.*)")
    private val parseTaskPluginRegEx = Regex("[📅⏳🛫✅❌]\\s*\\d{4}-\\d{2}-\\d{2}")
    private val parseTaskTimeRegEx = Regex("@(?:\\s*\\d{4}-\\d{2}-\\d{2})?\\s*\\d\\d?:\\d{2}(?:\\s*-\\s*\\d\\d?:\\d{2})?")
    private val prioritySymbolsRegEx = Regex("[🔺⏫🔼🔽⏬]")

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val dateFormatterYear = DateTimeFormatter.ofPattern("MMM d, uuuu")

    fun getWidgetTasks(tasks: MutableList<Task>, date: String): ArrayList<TaskWidgetDisplayData>{
        val displayTasks: ArrayList<TaskWidgetDisplayData> = arrayListOf()

        for(task in tasks){
            displayTasks.add(
                TaskWidgetDisplayData(
                    task = getParsedTaskText(task.task),
                    priority = getPrioritySymbol(task.priority),
                    summaryText = getWidgetSummaryText(task, date),
                )
            )
        }

        return displayTasks
    }

    private fun getWidgetSummaryText(task: Task, date: String): String{
        return when{
            task.evDate == date && task.evStartTime != null && task.evEndTime != null -> {
                "${task.evStartTime} - ${task.evEndTime}"
            }
            task.evDate == date && task.evStartTime != null -> {
                task.evStartTime
            }
            else -> {
                // if it is not an event or it is not today
                getTaskSummaryText(task, date)
            }
        }
    }

    fun getTasks(tasks: MutableList<Task>, date: String): MutableList<TaskDisplayData>{
        val displayTasks: MutableList<TaskDisplayData> = mutableListOf()

        for(task in tasks){
            displayTasks.add(
                TaskDisplayData(
                task = getParsedTaskText(task.task),
                taskSummary = getTaskSummaryText(task, date),
                status = getStatusName(task.status),
                priority = getPriorityString(task.priority),
                prioritySymbol = getPrioritySymbol(task.priority),
                startDate = getDateString(task.startDate),
                scheduledDate = getDateString(task.scheduledDate),
                dueDate = getDateString(task.dueDate),
                evDate = getDateString(task.evDate),
                evStartTime = task.evStartTime,
                evEndTime = task.evEndTime,
                evDateTimeString = getEventDateTimeString(task.evDate, task.evStartTime, task.evEndTime, date),
                isChecked = task.status == "x",
                evIsToday = date == task.evDate,
                priorityNumber = task.priority.ordinal,
            )
            )
        }

        return displayTasks
    }

    private fun getParsedTaskText(task: String): String{
        var text: String = parseTextRegEx.find(task)?.groups?.get("text")?.value ?: ""
        text = text.replace(parseTaskPluginRegEx, "")
        text = text.replace(parseTaskTimeRegEx, "")
        text = text.replace(prioritySymbolsRegEx, "")
        text = text.replace(settings.tasksTag, "")
        text = text.trim()

        return text
    }

    private fun getTaskSummaryText(task: Task, dateString: String): String {
        val date = LocalDate.parse(dateString)

        return when{
            task.dueDate == dateString -> "Due today"
            task.startDate == dateString -> "Starts today"
            task.dueDate != null -> {
                val parsedDueDate = LocalDate.parse(task.dueDate)
                val formattedDate = if(date.year == parsedDueDate.year)
                    parsedDueDate.format(dateFormatter) else parsedDueDate.format(dateFormatterYear)
                "Due $formattedDate"
            }
            task.startDate != null -> {
                val parsedStartDate = LocalDate.parse(task.startDate)
                val formattedDate = if(date.year == parsedStartDate.year)
                    parsedStartDate.format(dateFormatter) else parsedStartDate.format(dateFormatterYear)
                "Starts $formattedDate"
            }
            task.evDate != null && task.evDate != dateString -> {
                val parsedEvDate = LocalDate.parse(task.evDate)
                val formattedDate = if(date.year == parsedEvDate.year)
                    parsedEvDate.format(dateFormatter) else parsedEvDate.format(dateFormatterYear)
                "Event on $formattedDate"
            }
            task.scheduledDate == dateString -> "Scheduled today"
            else -> ""
        }
    }

    // FIXME Hacer con un map con todos los bindings para que pueda agregar los custom
    private fun getStatusName(status: String?): String{
        return when (status){
            " " -> "To Do"
            "x" -> "Done"
            "/" -> "In Progress"
            "-" -> "Cancelled"
            else -> ""
        }/*.replaceFirstChar { // capitalize first word if not already
            if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString()
        }*/
    }

    private fun getPriorityString(priority: TaskPriority): String{
        return when(priority){
            TaskPriority.HIGHEST -> "Highest Priority"
            TaskPriority.HIGH -> "High Priority"
            TaskPriority.MEDIUM -> "Medium Priority"
            TaskPriority.NORMAL -> ""
            TaskPriority.LOW -> "Low Priority"
            TaskPriority.LOWEST -> "Lowest Priority"
        }
    }

    private fun getPrioritySymbol(priority: TaskPriority): String{
        return when(priority){
            TaskPriority.HIGHEST -> "🔺"
            TaskPriority.HIGH -> "⏫"
            TaskPriority.MEDIUM -> "🔼"
            TaskPriority.NORMAL -> ""
            TaskPriority.LOW -> "🔽"
            TaskPriority.LOWEST -> "⏬"
        }
    }

    // TODO cambiar el formato de las fechas en las que las mostramos en las dos funciones de abajo
    private fun getDateString(date: String?): String{
        if(date == null) return "N/A"

        return LocalDate.parse(date).format(dateFormatterYear)
    }

    private fun getEventDateTimeString(evDate: String?, evStartTime: String?, evEndTime: String?, today: String): String{
        val evDateFormatted = if(evDate != null) LocalDate.parse(evDate).format(dateFormatter) else ""

        return if (evDate == null){
            ""
        }else if(evDate == today){
            if(evEndTime != null) "Event lasts from $evStartTime to $evEndTime" else "Event starts at $evStartTime"
        }else{
            if(evEndTime != null) "Event lasts from $evStartTime to $evEndTime on $evDateFormatted"
            else "Event starts on $evDateFormatted at $evStartTime"
        }
    }


}