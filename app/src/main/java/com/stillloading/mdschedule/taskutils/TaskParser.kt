package com.stillloading.mdschedule.taskutils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.stillloading.mdschedule.data.FileData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskPriority
import com.stillloading.mdschedule.data.UnParsedTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeParseException

class TaskParser(private val context: Context, private val settings: SettingsData) {

    private val TAG = "Md Companion Debug"
    //private val tasksTag = settings.getString("tasks_tag")
    //private var skipDirectories = JSONArray(settings.getString("skip_directories"))


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


    suspend fun getTasks(date: LocalDate, startingUri: Uri): MutableList<Task>{
        return withContext(Dispatchers.IO){
            val (files, todaysFile) = getAllFiles(startingUri, date)
            val (rawTasks, rawDayPlannerTasks) = searchFiles(files, todaysFile)
            if(rawTasks.size > 0){
                return@withContext parseTasks(rawTasks, rawDayPlannerTasks, date)
            }
            return@withContext mutableListOf()
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


    // Return Mutable Lists of Triple(Title, line/task, uri) and Pair(line/task, uri)
    private fun searchFiles(files: MutableList<FileData>, todaysFile: Uri?):
            Pair<MutableList<UnParsedTask>, MutableList<UnParsedTask>>{

        val contentResolver = context.contentResolver
        val tasks: MutableList<UnParsedTask> = mutableListOf()
        val dayPlannerTasks: MutableList<UnParsedTask> = mutableListOf()

        var line: String?

        for(i in 0..<files.size) {
            contentResolver.openInputStream(files[i].uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    line = reader.readLine()
                    while (line != null) {
                        if (isLineTask(line!!)) tasks.add(UnParsedTask(files[i].title, line!!, files[i].uri))

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
                        if (isLineDayPlanner(line!!)) dayPlannerTasks.add(UnParsedTask(null, line!!, todaysFile))

                        line = reader.readLine()
                    }
                }
            }
        }


        return Pair(tasks, dayPlannerTasks)
    }

    private fun isLineTask(line: String): Boolean{
        return tasksRegEx.containsMatchIn(line)
    }

    private fun isLineDayPlanner(line: String): Boolean{
        return dayPlannerRegEx.containsMatchIn(line)
    }

    private fun parseTasks(tasks: MutableList<UnParsedTask>, dayPlannerTasks: MutableList<UnParsedTask>,
                           date: LocalDate): MutableList<Task>{
        val dateString = date.toString()

        var title: String?
        var text: String
        var uri: Uri

        var matches: Sequence<MatchResult>
        var statusAndPlannerMatch: MatchResult?
        val parsedTasks: MutableList<Task> = mutableListOf()


        var evDate: String? = null
        var evStartTime: String? = null
        var evEndTime: String? = null

        var priority: TaskPriority
        var status: String? = null
        var dueDate: String? = null
        var scheduledDate: String? = null
        var startDate: String? = null

        // Day Planner tasks
        for(task in dayPlannerTasks){
            text = task.task
            uri = task.uri

            statusAndPlannerMatch = taskStatusAndPlannerRegEx.find(text)
            status = statusAndPlannerMatch?.groups?.get("status")?.value
            evStartTime = statusAndPlannerMatch?.groups?.get("startTime")?.value
            evEndTime = statusAndPlannerMatch?.groups?.get("endTime")?.value


            parsedTasks.add(
                Task(
                task = text,
                status = status,
                scheduledDate = dateString,
                evDate = dateString,
                evStartTime = evStartTime,
                evEndTime = evEndTime,
                uri = uri
            )
            )
        }


        // tasks from tasks plugin
        for(task in tasks){
            title = task.fileTitle
            text = task.task
            uri = task.uri

            evDate = null
            evStartTime = null
            evEndTime = null

            priority = TaskPriority.NORMAL
            dueDate = null
            scheduledDate = null
            startDate = null

            // FIXME Optimizacion: Ir yo con un for desde atras del string agarrando una por una.
            matches = taskParseRegEx.findAll(text)
            matches.forEach { match ->
                //Log.i(TAG, match.groupValues.toString())
                //Log.i(TAG, match.groups["symbol"]?.value.toString())
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
                        evStartTime = match.groups["startTime"]?.value
                        evEndTime = match.groups["endTime"]?.value
                        evDate = if(evStartTime != null) match.groups["date"]?.value else null
                    }
                    else -> Log.i(TAG, "Symbol: ${match.groups["symbol"]?.value.toString()} not implemented")
                }
            }


            // if no scheduled date, make the scheduled date the note title if applicable
            if (scheduledDate == null){
                try {
                    LocalDate.parse(title)
                    scheduledDate = title
                }catch (_: DateTimeParseException) {}
            }

            // If not in date
            if((dateString != startDate) and (dateString != scheduledDate) and (dateString != dueDate)
                        and (dateString != evDate) and (!taskIsInProgress(date, startDate, dueDate))){
                continue
            }

            if(evStartTime != null && evDate == null) evDate = getEventDate(dateString, startDate, scheduledDate, dueDate)

            statusAndPlannerMatch = taskStatusRegEx.find(text)
            status = statusAndPlannerMatch?.groups?.get("status")?.value

            parsedTasks.add(
                Task(
                task = text,
                priority = priority,
                status = status,
                dueDate = dueDate,
                scheduledDate = scheduledDate,
                startDate = startDate,
                evDate = evDate,
                evStartTime = evStartTime,
                evEndTime = evEndTime,
                uri = uri,
            )
            )
        }

        return parsedTasks
    }


    private fun taskIsInProgress(date: LocalDate, startDate: String?, dueDate: String?): Boolean{
        if((startDate == null) or (dueDate == null)) return false

        return try{
            // checks if the date is between the start or due date (inclusive in both sides)
            //!(date.isBefore(LocalDate.parse(startDate)) || date.isAfter(LocalDate.parse(dueDate)))

            date.isBefore(LocalDate.parse(dueDate)) && date.isAfter(LocalDate.parse(startDate))
        }catch (_: DateTimeParseException) {
            false
        }
    }


    // returns which is supposed to be treated as the event date if none was specified
    private fun getEventDate(date: String, startDate: String?, scheduledDate: String?, dueDate: String?): String?{
        return when{
            startDate != null -> startDate
            dueDate != null -> dueDate
            scheduledDate != null -> scheduledDate
            else -> null
        }
        /*
        if(date == startDate){
            return startDate
        }else if (startDate == null && date == dueDate){
            return dueDate
        }else if(startDate == null && dueDate == null && date == scheduledDate){
            return scheduledDate
        }

        return null
         */
    }
}