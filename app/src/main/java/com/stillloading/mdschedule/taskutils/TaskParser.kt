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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.stillloading.mdschedule.data.FileData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskDates
import com.stillloading.mdschedule.data.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeParseException

class TaskParser(private val context: Context, private val settings: SettingsData) {

    private val TAG = "TaskParser"


    private val tasksRegEx = Regex("^\\s*- \\[.].+${settings.tasksTag}")
    private val dayPlannerRegEx = Regex("^\\s*(?:- \\[.]|-)\\s+\\d\\d?:\\d{2}\\s*-\\s*\\d\\d?:\\d{2}")

    private val taskStatusRegEx = Regex("^\\s*- \\[(?<status>.)]")
    private val taskStatusAndPlannerRegEx = Regex("^\\s*- (?:\\[(?<status>.)])?(?:\\s*(?<startTime>\\d\\d?:\\d{2})(?:\\s*-\\s*(?<endTime>\\d\\d?:\\d{2}))?)?")
    private val taskParseRegEx = Regex("""
        \s*(?<symbol>[@📅⏳🛫🔺⏫🔼🔽⏬])
        \s*(?<date>\d{4}-\d{2}-\d{2})?
        \s*(?<startTime>\d\d?:\d{2})?
        \s*-?\s*(?<endTime>\d\d?:\d{2})?
    """.trimIndent(), RegexOption.COMMENTS)

    private val getTimeRegEx = Regex("^(?<hour>\\d\\d?):(?<minutes>\\d{2})")

    private val lastValidTime = "23:59"


    suspend fun getTasks(date: LocalDate, startingUri: Uri): MutableList<Task>{
        return withContext(Dispatchers.IO){
            val (files, todaysFile) = getAllFiles(startingUri, date)
            return@withContext searchFiles(files, todaysFile, date)
        }
    }


    private fun skipDirectory(directory: DocumentFile): Boolean{
        for(dir in settings.skipDirectories){
            if(dir == directory.name) return true
        }

        return false
    }


    // Returns Pair(list of files(title, uri), uri of todays file)
    private fun getAllFiles(startingUri: Uri, date: LocalDate): Pair<MutableList<FileData>, Uri?> {
        val dFile = DocumentFile.fromTreeUri(context, startingUri) ?: return Pair(mutableListOf(), null)

        val stack: MutableList<DocumentFile> = mutableListOf(dFile)
        val mdFiles: MutableList<FileData> = mutableListOf()

        var todaysFile: Uri? = null
        var fileTitle: String?

        do {
            val directory = stack.removeLast()

            if(skipDirectory(directory)) continue

            val documentFilesList = directory.listFiles()

            for (file in documentFilesList) {
                if (file.isDirectory) {
                    stack.add(file)
                } else if (file.type == "text/markdown") {
                    fileTitle = file.name?.dropLast(3)
                    mdFiles.add(FileData(fileTitle, file.uri))
                    try{
                        if(LocalDate.parse(fileTitle) == date) todaysFile = file.uri
                    }catch (_: DateTimeParseException) {}
                }
            }


        } while (stack.size > 0)

        return Pair(mdFiles, todaysFile)
    }


    private fun searchFiles(files: MutableList<FileData>, todaysFile: Uri?, date: LocalDate):
            MutableList<Task>{

        val contentResolver = context.contentResolver

        val tasks = mutableListOf<Task>()
        val dateString = date.toString()

        var line: String?

        for(i in 0..<files.size) {
            contentResolver.openInputStream(files[i].uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    line = reader.readLine()
                    while (line != null) {
                        if (isLineTask(line!!)) {
                            val task = getParsedTask(files[i].title, line!!, files[i].uri, date, dateString)
                            if(task != null){
                                tasks.add(task)
                            }
                        }

                        line = reader.readLine()
                    }
                }
            }
        }

        if (todaysFile != null) {
            contentResolver.openInputStream(todaysFile)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    line = reader.readLine()
                    while (line != null){
                        if (isLineDayPlanner(line!!)){
                            val dayPlannerTask = getParsedDayPlannerTask(line!!, todaysFile, dateString)
                            if(dayPlannerTask != null){
                                tasks.add(dayPlannerTask)
                            }
                        }

                        line = reader.readLine()
                    }
                }
            }
        }


        return tasks
    }

    private fun isLineTask(line: String): Boolean{
        return tasksRegEx.containsMatchIn(line)
    }

    private fun isLineDayPlanner(line: String): Boolean{
        return dayPlannerRegEx.containsMatchIn(line)
    }



    private fun getParsedTask(title: String?, task: String, uri: Uri, date: LocalDate, dateString: String): Task?{
        var evDate: String? = null
        var evStartTime: String? = null
        var evEndTime: String? = null

        var priority = TaskPriority.NORMAL
        var dueDate: String? = null
        var scheduledDate: String? = null
        var startDate: String? = null


        val matches = taskParseRegEx.findAll(task)
        matches.forEach { match ->
            when(match.groups["symbol"]?.value){
                "📅" -> {
                    dueDate = match.groups["date"]?.value
                }
                "⏳" -> {
                    scheduledDate = match.groups["date"]?.value
                }
                "🛫" -> {
                    startDate = match.groups["date"]?.value
                }
                "🔺" -> {
                    priority = TaskPriority.HIGHEST
                }
                "⏫" -> {
                    priority = TaskPriority.HIGH
                }
                "🔼" -> {
                    priority = TaskPriority.MEDIUM
                }
                "🔽" -> {
                    priority = TaskPriority.LOW
                }
                "⏬" -> {
                    priority = TaskPriority.LOWEST
                }
                "@" -> {
                    evDate = match.groups["date"]?.value
                    evStartTime = match.groups["startTime"]?.value
                    evEndTime = match.groups["endTime"]?.value
                }
                else -> Log.d(TAG, "Symbol: ${match.groups["symbol"]?.value.toString()} not implemented")
            }
        }


        // if no scheduled date, make the scheduled date the note title if applicable
        if (scheduledDate == null){
            scheduledDate = title
        }

        // If not in date
        if((dateString != startDate) and (dateString != scheduledDate) and (dateString != dueDate)
            and (dateString != evDate) and (!taskIsInProgress(date, startDate, dueDate))){
            return null
        }

        // from this point there are only going to be tasks that we are showing to the user
        // set the event date if there wasnt one specified
        if(evStartTime != null && evDate == null) evDate = getEventDate(startDate, scheduledDate, dueDate)

        // check if each date is valid before setting it
        val taskDates = TaskDates(
            startDate = startDate,
            scheduledDate = scheduledDate,
            dueDate = dueDate,
            evDate = evDate,
            evStartTime = evStartTime,
            evEndTime = evEndTime
        )
        // will remove or modify any date and time that is not valid
        checkValidDateTimes(taskDates)

        val statusAndPlannerMatch = taskStatusRegEx.find(task)
        val status = statusAndPlannerMatch?.groups?.get("status")?.value


        return Task(
            task = task,
            priority = priority,
            status = status,
            dueDate = taskDates.dueDate,
            scheduledDate = taskDates.scheduledDate,
            startDate = taskDates.startDate,
            evDate = taskDates.evDate,
            evStartTime = taskDates.evStartTime,
            evEndTime = taskDates.evEndTime,
            isDayPlanner = false,
            uri = uri,
            )

    }

    private fun getParsedDayPlannerTask(task: String, uri: Uri, dateString: String): Task?{
        val statusAndPlannerMatch = taskStatusAndPlannerRegEx.find(task)
        val status = statusAndPlannerMatch?.groups?.get("status")?.value
        val evStartTime = statusAndPlannerMatch?.groups?.get("startTime")?.value
        val evEndTime = statusAndPlannerMatch?.groups?.get("endTime")?.value

        // modify or remove any invalid times
        val taskDates = TaskDates(
            evStartTime = evStartTime,
            evEndTime = evEndTime
        )
        checkValidEventTimes(taskDates)

        if(taskDates.evStartTime == null){
            return null
        }


        return Task(
            task = task,
            status = status,
            scheduledDate = dateString,
            evDate = dateString,
            evStartTime = taskDates.evStartTime,
            evEndTime = taskDates.evEndTime,
            isDayPlanner = true,
            uri = uri
        )
    }



    private fun taskIsInProgress(date: LocalDate, startDate: String?, dueDate: String?): Boolean{
        if(!settings.inProgressTasksEnabled) return false
        if((startDate == null) or (dueDate == null)) return false

        return try{
            // checks if the date is between the start or due date (inclusive in both sides)
            //!(date.isBefore(LocalDate.parse(startDate)) || date.isAfter(LocalDate.parse(dueDate)))

            // checks if the date is between the start and due date (non inclusive)
            date.isBefore(LocalDate.parse(dueDate)) && date.isAfter(LocalDate.parse(startDate))
        }catch (_: DateTimeParseException) {
            false
        }
    }


    // returns which is supposed to be treated as the event date if none was specified
    private fun getEventDate(startDate: String?, scheduledDate: String?, dueDate: String?): String?{
        return when{
            startDate != null -> startDate
            dueDate != null -> dueDate
            scheduledDate != null -> scheduledDate
            else -> null
        }
    }


    private fun checkValidDateTimes(taskDates: TaskDates){
        taskDates.startDate = getValidDate(taskDates.startDate)
        taskDates.scheduledDate = getValidDate(taskDates.scheduledDate)
        taskDates.dueDate = getValidDate(taskDates.dueDate)
        taskDates.evDate = getValidDate(taskDates.evDate)

        if(taskDates.evDate == null){
            taskDates.evStartTime = null
            taskDates.evEndTime = null
        }else{
            checkValidEventTimes(taskDates)
        }
    }

    private fun checkValidEventTimes(taskDates: TaskDates){
        var (startHour, startMinutes) = getHourMinutes(taskDates.evStartTime)
        var (endHour, endMinutes) = getHourMinutes(taskDates.evEndTime)

        if(startMinutes == null) startMinutes = 0
        if(endMinutes == null) endMinutes = 0

        // if start time is greater than end time, invert them
        if(startHour != null && endHour != null){
            if((startHour > endHour) or (startHour == endHour && startMinutes > endMinutes)){
                val tempStartTime = taskDates.evStartTime
                val tempStartHour = startHour
                val tempStartMinutes = startMinutes

                taskDates.evStartTime = taskDates.evEndTime
                taskDates.evEndTime = tempStartTime

                // also update the local variables to perform the valid time check
                startHour = endHour
                startMinutes = endMinutes
                endHour = tempStartHour
                endMinutes = tempStartMinutes
            }
        }


        // assert times are valid
        taskDates.evStartTime = getValidTime(startHour, startMinutes, taskDates.evStartTime, isStartTime = true)
        taskDates.evEndTime = getValidTime(endHour, endMinutes, taskDates.evEndTime, isStartTime = false)


        // if start time and end time is the same, erase end time
        if(startHour == endHour && startMinutes == endMinutes){
            taskDates.evEndTime = null
        }
    }



    private fun getValidDate(date: String?): String?{
        if(date == null){
            return null
        }

        try{
            LocalDate.parse(date)
            return date
        }catch (_: DateTimeParseException){
            return null
        }
    }

    private fun getHourMinutes(time: String?): Pair<Int?, Int?>{
        if(time == null){
            return Pair(null, null)
        }

        val match = getTimeRegEx.find(time)
        val hour = match?.groups?.get("hour")?.value?.toIntOrNull()
        val minutes = match?.groups?.get("minutes")?.value?.toIntOrNull()

        return Pair(hour, minutes)
    }

    private fun getValidTime(hour: Int?, minutes: Int?, time: String?, isStartTime: Boolean = false): String?{
        if(hour == null || minutes == null) return null

        if(hour == 24 && minutes == 0){
            return lastValidTime
        }

        if(isStartTime && hour >= 24){
            return null
        }else if(!isStartTime && hour >= 24){
            return lastValidTime
        }

        if(minutes >= 60){
            return null
        }

        return time
    }

}