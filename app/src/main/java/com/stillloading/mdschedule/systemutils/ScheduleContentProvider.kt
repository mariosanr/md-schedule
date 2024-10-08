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

package com.stillloading.mdschedule.systemutils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.stillloading.mdschedule.backgroundutils.TaskAlarmManager
import com.stillloading.mdschedule.data.SettingsFlowData
import com.stillloading.mdschedule.data.toSettingsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

object ScheduleProviderContract{
    const val AUTHORITY = "com.stillloading.mdschedule.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    const val PATH_SETTINGS = "settings"
    const val PATH_TASKS = "tasks"
    const val PATH_LAST_UPDATED = "last_updated"
    const val PATH_UPDATING = "updating"


    const val CODE_ERROR = 0
    const val CODE_SUCCESS = 1
    const val CODE_UPDATING = 2

    object SETTINGS{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SETTINGS).build()

        const val DIRECTORIES = "directories"
        const val TASKS_TAG = "tasks_tag"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val DAY_PLANNER_NOTIFICATIONS_ENABLED = "day_planner_notifications_enabled"
        const val UPDATE_TIMES = "update_times"
        const val IN_PROGRESS_TASKS_ENABLED = "in_progress_tasks_enabled"
        const val DAY_PLANNER_WIDGET_ENABLED = "day_planner_widget_enabled"
        const val SKIP_DIRECTORIES = "skip_directories"
    }

    object TASKS{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TASKS).build()

        const val DATE = "date"

        const val COLUMN_UID = "uid"
        const val COLUMN_TASK = "task"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_STATUS = "status"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_SCHEDULED_DATE = "scheduled_date"
        const val COLUMN_START_DATE = "start_date"
        const val COLUMN_EV_DATE = "ev_date"
        const val COLUMN_EV_START_TIME = "ev_start_time"
        const val COLUMN_EV_END_TIME = "ev_end_time"
        const val COLUMN_IS_DAY_PLANNER = "is_day_planner"
        const val COLUMN_URI = "uri"
    }

    object LAST_UPDATED{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LAST_UPDATED).build()

        const val COLUMN_DATETIME = "date_time"
    }

    object UPDATING_TASKS{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_UPDATING).build()

        const val COLUMN_UPDATING = "is_updating"
    }

}

private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_SETTINGS, 1)
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_TASKS, 2)
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_LAST_UPDATED, 3)
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_UPDATING, 4)
}

class ScheduleContentProvider : ContentProvider() {

    private val TAG = "ScheduleContentProvider"

    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var taskAlarmManager: TaskAlarmManager

    //Room database
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var taskDao: TaskDao
    private var updatingDB = false

    // Preferences Data Store
    private lateinit var settingsFlowData: SettingsFlowData
    private lateinit var lastUpdatedFlow: Flow<String>


    override fun onCreate(): Boolean {
        fileSystemManager = FileSystemManager(context!!)
        taskAlarmManager = TaskAlarmManager(context!!.applicationContext)

        taskDatabase = TaskDatabase.getDatabase(context!!)
        taskDao = taskDatabase.taskDao()

        settingsFlowData = fileSystemManager.getSettingsFlow()
        lastUpdatedFlow = fileSystemManager.getLastUpdatedFlow()

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        context?.enforceCallingOrSelfPermission("com.stillloading.mdschedule.provider.READ_PROVIDER", "Read permission denied")
        return when(uriMatcher.match(uri)){
            1 -> { // settings
                runBlocking(Dispatchers.IO) {
                    val cursor = MatrixCursor(arrayOf("key", "value"))

                    val settingsMap = fileSystemManager.getSettingsMap(settingsFlowData)


                    settingsMap.forEach { (key, value) ->
                        cursor.addRow(arrayOf(key, value))
                    }

                    // Returns the as strings. The settings that are lists are a comma separated string
                    cursor
                }
            }
            2 -> { // tasks
                taskDao.getAll()
            }
            3 -> {
                runBlocking(Dispatchers.IO) {
                    val cursor = MatrixCursor(arrayOf(ScheduleProviderContract.LAST_UPDATED.COLUMN_DATETIME)).apply {
                        addRow(arrayOf(fileSystemManager.getLastUpdated(lastUpdatedFlow)))
                    }

                    cursor
                }
            }
            4 -> {
                val cursor = MatrixCursor(arrayOf(ScheduleProviderContract.UPDATING_TASKS.COLUMN_UPDATING)).apply {
                    addRow(arrayOf(updatingDB.toString()))
                }

                cursor
            }
            else -> { // not recognized
                throw IllegalArgumentException()
            }
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        context?.enforceCallingOrSelfPermission("com.stillloading.mdschedule.provider.WRITE_PROVIDER", "Write permission denied")
        return when(uriMatcher.match(uri)){
            1 -> { // settings
                runBlocking(Dispatchers.IO){
                    if(values != null){
                        /*
                        fileSystemManager.cancelUpdateTimes(
                            fileSystemManager.getSettingsData(settingsFlowData),
                            taskAlarmManager
                        )
                         */

                        values.toSettingsData().let { fileSystemManager.saveSettings(it) }
                    }

                    /*
                    fileSystemManager.setUpdateTimes(
                        fileSystemManager.getSettingsData(settingsFlowData),
                        taskAlarmManager
                    )
                     */
                }

                context?.contentResolver?.notifyChange(uri, null)
                ScheduleProviderContract.CODE_SUCCESS
            }
            2 -> { // tasks
                if(!updatingDB){
                    updatingDB = true
                    context?.contentResolver?.notifyChange(ScheduleProviderContract.UPDATING_TASKS.CONTENT_URI, null)


                    runBlocking(Dispatchers.IO) {
                        val date = values?.getAsString(ScheduleProviderContract.TASKS.DATE)
                        if(date != null){
                            val settings = fileSystemManager.getSettingsData(settingsFlowData)


                            val tasksArray = fileSystemManager.getTasksArray(settings, date)

                            // wait for operation to complete to cancel notifications and delete database
                            val taskCount = taskDao.getCount()
                            fileSystemManager.cancelTaskNotifications(taskCount, taskAlarmManager)

                            taskDao.deleteAll()


                            taskDao.insertAll(*tasksArray)
                            fileSystemManager.saveLastUpdated(LocalDateTime.now())

                            fileSystemManager.setTaskNotifications(tasksArray, settings, taskAlarmManager)
                        }
                    }
                    updatingDB = false
                    context?.contentResolver?.notifyChange(ScheduleProviderContract.UPDATING_TASKS.CONTENT_URI, null)

                    context?.contentResolver?.notifyChange(uri, null)
                    return ScheduleProviderContract.CODE_SUCCESS
                }else{
                    return ScheduleProviderContract.CODE_UPDATING
                }
            }
            else -> { // not recognized
                ScheduleProviderContract.CODE_ERROR
            }
        }
    }
}