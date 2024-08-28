package com.stillloading.mdschedule.systemutils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.database.getStringOrNull
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.stillloading.mdschedule.TasksWidget
import com.stillloading.mdschedule.backgroundutils.UpdateTasksWorker
import com.stillloading.mdschedule.data.DirectoryData
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

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


    suspend fun getWidgetTasks(): MutableList<TaskWidgetDisplayData>{
        return withContext(Dispatchers.IO){

            val today = LocalDate.now().toString()

            val tasks = getTasksList()

            val settings = getSettings().toSettingsData()
            val taskDisplayManager = TaskDisplayManager(settings)

            taskDisplayManager.getWidgetTasks(tasks, today)
        }
    }


    // Return Pair(timeTasks, nonTimeTasks)
    suspend fun getTasks(date: String, update: Boolean = false): Pair<MutableList<TaskDisplayData>, MutableList<TaskDisplayData>>? {
        return withContext(Dispatchers.IO){

            if(update) {
                updateTasks(date, true) ?: return@withContext null
            }

            val tasks = getTasksList()

            val timeTasks: MutableList<Task> = mutableListOf()
            val nonTimeTasks: MutableList<Task> = mutableListOf()

            for(task in tasks){
                if(task.evDate == date && task.evStartTime != null){
                    timeTasks.add(task)
                }else{
                    nonTimeTasks.add(task)
                }
            }

            val settings = getSettings().toSettingsData()

            val taskDisplayManager = TaskDisplayManager(settings)

            Pair(taskDisplayManager.getTasks(timeTasks, date), taskDisplayManager.getTasks(nonTimeTasks, date))
        }
    }


    suspend fun updateTasks(date: String, notifiyError: Boolean): Int?{
        val taskUpdateValues = ContentValues().apply {
            put(ScheduleProviderContract.TASKS.DATE, date)
        }

        val responseCode = context.contentResolver.update(
            ScheduleProviderContract.TASKS.CONTENT_URI, taskUpdateValues, null, null
        )
        if(responseCode == ScheduleProviderContract.CODE_UPDATING){
            if(notifiyError){
                showRefreshingToast()
            }
            return null
        }

        // update the widgets when the tasks are updated on the app
        updateWidgets()

        return ScheduleProviderContract.CODE_SUCCESS
    }


    private suspend fun showRefreshingToast(){
        withContext(Dispatchers.Main){
            Toast.makeText(context, "Already refreshing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWidgets(){
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TasksWidget::class.java)

        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        val updateWidgetsIntent = Intent(context, TasksWidget::class.java).apply {
            setAction("android.appwidget.action.APPWIDGET_UPDATE")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(updateWidgetsIntent)
    }


    private fun getTasksList(): MutableList<Task>{
        val tasksCursor = context.contentResolver.query(
            ScheduleProviderContract.TASKS.CONTENT_URI, null, null, null, null
        )

        val tasks = mutableListOf<Task>()

        tasksCursor?.apply {

            //val idColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_UID)

            val taskColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_TASK)
            val priorityColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_PRIORITY)
            val statusColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_STATUS)
            val dueDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_DUE_DATE)
            val scheduledDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_SCHEDULED_DATE)
            val startDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_START_DATE)
            val evDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_DATE)
            val evStartTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_START_TIME)
            val evEndTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_END_TIME)
            val isDayPlannerColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_IS_DAY_PLANNER)
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
                    isDayPlanner = getStringOrNull(isDayPlannerColumn).toBoolean(),
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

    fun getLastUpdatedString(): String?{
        val lastUpdated = getLastUpdated() ?: return null

        val dateTimeFormatter = if(lastUpdated.toLocalDate().isEqual(LocalDate.now()))
            DateTimeFormatter.ofPattern("'Last updated at' HH:mm") else
            DateTimeFormatter.ofPattern("'Last updated on' MMM d 'at' HH:mm")

        return lastUpdated.format(dateTimeFormatter)
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