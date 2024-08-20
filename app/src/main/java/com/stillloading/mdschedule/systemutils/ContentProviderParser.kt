package com.stillloading.mdschedule.systemutils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.database.getStringOrNull
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.data.TaskPriority
import com.stillloading.mdschedule.data.toContentValues
import com.stillloading.mdschedule.taskutils.TaskDisplayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ContentProviderParser(
    private val context: Context
) {

    fun saveSettings(directoryList: MutableList<DirectoryData>){
        val settings = SettingsData(
            directories = directoryList.mapNotNull { it.uri }.toSet(),
            tasksTag = "#todo",
            skipDirectories = setOf(".obsidian", ".trash")
        )

        context.contentResolver.update(
            ScheduleProviderContract.SETTINGS.CONTENT_URI, settings.toContentValues(), null, null
        )

    }


    // Return Pair(timeTasks, nonTimeTasks)
    suspend fun getTasks(settings: SettingsData, update: Boolean = false): Pair<MutableList<TaskDisplayData>, MutableList<TaskDisplayData>> {
        return withContext(Dispatchers.IO){

            val today = LocalDate.now().toString()

            if(update){
                val taskUpdateValues = ContentValues().apply {
                    put(ScheduleProviderContract.TASKS.DATE, today)
                }

                context.contentResolver.update(
                    ScheduleProviderContract.TASKS.CONTENT_URI, taskUpdateValues, null, null
                )
            }

            val tasksCursor = context.contentResolver.query(
                ScheduleProviderContract.TASKS.CONTENT_URI, null, null, null, null
            )

            val tasks = mutableListOf<Task>()

            // TODO go through cursor
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
            }?.close() // FIXME it could be more efficient to requery the cursor instead of opening a new one every time


            val timeTasks: MutableList<Task> = mutableListOf()
            val nonTimeTasks: MutableList<Task> = mutableListOf()

            for(task in tasks){
                if(task.evDate == today && task.evStartTime != null){
                    timeTasks.add(task)
                }else{
                    nonTimeTasks.add(task)
                }
            }

            val taskDisplayManager = TaskDisplayManager(settings)

            Pair(taskDisplayManager.getTasks(timeTasks, today), taskDisplayManager.getTasks(nonTimeTasks, today))
        }
    }

}