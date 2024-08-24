package com.stillloading.mdschedule.systemutils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.database.getStringOrNull
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.SettingsDisplayData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.data.TaskPriority
import com.stillloading.mdschedule.data.TaskWidgetDisplayData
import com.stillloading.mdschedule.data.toContentValues
import com.stillloading.mdschedule.data.toSettingsData
import com.stillloading.mdschedule.taskutils.TaskDisplayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class ContentProviderParser(
    private val context: Context
) {

    val TAG = "Content Provider Parser"

    fun getSettings(): SettingsDisplayData {
        val cursor = context.contentResolver.query(
            ScheduleProviderContract.SETTINGS.CONTENT_URI, null, null, null, null
        )

        val settings = SettingsDisplayData()

        cursor?.apply {

            val directoriesKey = ScheduleProviderContract.SETTINGS.DIRECTORIES
            val tasksTagKey = ScheduleProviderContract.SETTINGS.TASKS_TAG
            val skipDirectoriesKey = ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES

            while(moveToNext()){
                val key = getString(0)
                val value = getString(1)

                when{
                    key == directoriesKey && value != "" -> {
                        settings.directories = value.split(",").map {
                            val uri = Uri.parse(it)
                            DirectoryData(
                                uri = uri,
                                text = uri.path.toString().substringAfter(":")
                            )
                        }.toMutableList()
                    }
                    key == tasksTagKey -> {
                        settings.tasksTag = value
                    }
                    key == skipDirectoriesKey && value != "" -> {
                        settings.skipDirectories = value.split(",").toMutableList()
                    }
                    else -> {}
                }
            }
        }?.close()

        return settings
    }



    fun saveSettings(settings: SettingsDisplayData){
        context.contentResolver.update(
            ScheduleProviderContract.SETTINGS.CONTENT_URI, settings.toContentValues(), null, null
        )

    }


    suspend fun getWidgetTasks(update: Boolean = false): ArrayList<TaskWidgetDisplayData>?{
        return withContext(Dispatchers.IO){

            val today = LocalDate.now().toString()

            if(update){
                updateTasks(today) ?: return@withContext null
            }

            val tasks = getTasksList()

            // TODO order them



            val settings = getSettings().toSettingsData()
            val taskDisplayManager = TaskDisplayManager(settings)

            taskDisplayManager.getWidgetTasks(tasks, today)
        }
    }


    // Return Pair(timeTasks, nonTimeTasks)
    suspend fun getTasks(update: Boolean = false): Pair<MutableList<TaskDisplayData>, MutableList<TaskDisplayData>>? {
        return withContext(Dispatchers.IO){

            val today = LocalDate.now().toString()

            if(update){
                updateTasks(today) ?: return@withContext null
            }

            val tasks = getTasksList()

            // FIXME It would be more efficient if the content provider returned the task display data
            //  Because here we need to get the settings again, while the content provider already has them loaded.
            //  This is unnecesary if that change is made
            val timeTasks: MutableList<Task> = mutableListOf()
            val nonTimeTasks: MutableList<Task> = mutableListOf()

            for(task in tasks){
                if(task.evDate == today && task.evStartTime != null){
                    timeTasks.add(task)
                }else{
                    nonTimeTasks.add(task)
                }
            }

            val settings = getSettings().toSettingsData()

            val taskDisplayManager = TaskDisplayManager(settings)

            Pair(taskDisplayManager.getTasks(timeTasks, today), taskDisplayManager.getTasks(nonTimeTasks, today))
        }
    }


    // FIXME do this through Work Manager instead to be sure it finishes.
    suspend fun updateTasks(today: String): Int?{
        val taskUpdateValues = ContentValues().apply {
            put(ScheduleProviderContract.TASKS.DATE, today)
        }

        val responseCode = context.contentResolver.update(
            ScheduleProviderContract.TASKS.CONTENT_URI, taskUpdateValues, null, null
        )
        if(responseCode == ScheduleProviderContract.CODE_UPDATING){
            withContext(Dispatchers.Main){
                Toast.makeText(context, "Already refreshing", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        return ScheduleProviderContract.CODE_SUCCESS
    }


    private fun getTasksList(): MutableList<Task>{
        val tasksCursor = context.contentResolver.query(
            ScheduleProviderContract.TASKS.CONTENT_URI, null, null, null, null
        )

        val tasks = mutableListOf<Task>()

        tasksCursor?.apply {

            val taskColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_TASK)
            val priorityColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_PRIORITY)
            val statusColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_STATUS)
            val dueDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_DUE_DATE)
            val scheduledDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_SCHEDULED_DATE)
            val startDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_START_DATE)
            val evDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_DATE)
            val evStartTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_START_TIME)
            val evEndTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_END_TIME)
            val uriColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_URI)


            while (moveToNext()){
                tasks.add(Task(
                    task = getString(taskColumn),
                    priority = TaskPriority.entries.getOrNull(getInt(priorityColumn)) ?: TaskPriority.NORMAL,
                    status = getStringOrNull(statusColumn),
                    dueDate = getStringOrNull(dueDateColumn),
                    scheduledDate = getStringOrNull(scheduledDateColumn),
                    startDate = getStringOrNull(startDateColumn),
                    evDate = getStringOrNull(evDateColumn),
                    evStartTime = getStringOrNull(evStartTimeColumn),
                    evEndTime = getStringOrNull(evEndTimeColumn),
                    uri = getStringOrNull(uriColumn)?.let { Uri.parse(it) }
                ))
            }
        }?.close()

        return tasks
    }



    fun getLastUpdated(): LocalDateTime?{
        var lastUpdated: LocalDateTime? = null
        context.contentResolver.query(
            ScheduleProviderContract.LAST_UPDATED.CONTENT_URI, null, null, null, null
        )?.apply {

            val dateTimeColumn = getColumnIndex(ScheduleProviderContract.LAST_UPDATED.COLUMN_DATETIME)

            if(moveToFirst()){
                val dateTime = getString(dateTimeColumn)
                if(dateTime != "null"){
                    try {
                        lastUpdated = LocalDateTime.parse(dateTime)
                    }catch (_: DateTimeParseException){}
                }
            }
        }?.close()

        return lastUpdated
    }

    fun getIsUpdatingTasks(): Boolean{
        var isUpdating = false

        context.contentResolver.query(
            ScheduleProviderContract.UPDATING_TASKS.CONTENT_URI, null, null, null, null
        )?.apply {
            val isUpdatingColumn = getColumnIndex(ScheduleProviderContract.UPDATING_TASKS.COLUMN_UPDATING)

            if(moveToFirst()){
                isUpdating = getStringOrNull(isUpdatingColumn).toBoolean()
            }
        }?.close()

        return isUpdating
    }

}