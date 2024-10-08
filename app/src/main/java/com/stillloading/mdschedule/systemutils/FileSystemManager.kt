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

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stillloading.mdschedule.backgroundutils.TaskAlarmManager
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.SettingsFlowData
import com.stillloading.mdschedule.taskutils.TaskDisplayManager
import com.stillloading.mdschedule.taskutils.TaskParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class FileSystemManager(
    private val context: Context
) {

    companion object {
        private val DEFAULT_DIRECTORIES = setOf<String>()
        private val DEFAULT_TASKS_TAG = ""
        private val DEFAULT_NOTIFICATIONS_ENABLED = false
        private val DEFAULT_DAY_PLANNER_NOTIFICATIONS_ENABLED = true
        private val DEFAULT_UPDATE_TIMES = setOf("00:00")
        private val DEFAULT_IN_PROGRESS_TASKS_ENABLED =  true
        private val DEFAULT_DAY_PLANNER_WIDGET_ENABLED = true
        private val DEFAULT_SKIP_DIRECTORIES = setOf(".obsidian", ".trash")

        private val DEFAULT_LAST_UPDATED = "null"

        private const val directoriesName = ScheduleProviderContract.SETTINGS.DIRECTORIES
        private const val tasksTagName = ScheduleProviderContract.SETTINGS.TASKS_TAG
        private const val notificationsEnabledName = ScheduleProviderContract.SETTINGS.NOTIFICATIONS_ENABLED
        private const val dayPlannerNotificationsEnabledName = ScheduleProviderContract.SETTINGS.DAY_PLANNER_NOTIFICATIONS_ENABLED
        private const val updateTimesName = ScheduleProviderContract.SETTINGS.UPDATE_TIMES
        private const val inProgressTasksEnabledName = ScheduleProviderContract.SETTINGS.IN_PROGRESS_TASKS_ENABLED
        private const val dayPlannerWidgetEnabledName = ScheduleProviderContract.SETTINGS.DAY_PLANNER_WIDGET_ENABLED
        private const val skipDirectoriesName = ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES

        private const val lastUpdatedName = ScheduleProviderContract.LAST_UPDATED.COLUMN_DATETIME

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        private val directoriesKey = stringSetPreferencesKey(directoriesName)
        private val tasksTagKey = stringPreferencesKey(tasksTagName)
        private val notificationsEnabledKey = booleanPreferencesKey(notificationsEnabledName)
        private val dayPlannerNotificationsEnabledKey = booleanPreferencesKey(dayPlannerNotificationsEnabledName)
        private val updateTimesKey = stringSetPreferencesKey(updateTimesName)
        private val inProgressTasksEnabledKey = booleanPreferencesKey(inProgressTasksEnabledName)
        private val dayPlannerWidgetEnabledKey = booleanPreferencesKey(dayPlannerWidgetEnabledName)
        private val skipDirectoriesKey = stringSetPreferencesKey(skipDirectoriesName)

        private val lastUpdatedKey = stringPreferencesKey(lastUpdatedName)
    }

    suspend fun saveSettings(settingsData: SettingsData) {
        context.dataStore.edit { settings ->

            settings[directoriesKey] = settingsData.directories.map {
                it.toString()
            }.toSet()
            settings[tasksTagKey] = settingsData.tasksTag
            settings[notificationsEnabledKey] = settingsData.notificationsEnabled
            settings[dayPlannerNotificationsEnabledKey] = settingsData.dayPlannerNotificationsEnabled
            settings[updateTimesKey] = settingsData.updateTimes
            settings[inProgressTasksEnabledKey] = settingsData.inProgressTasksEnabled
            settings[dayPlannerWidgetEnabledKey] = settingsData.dayPlannerWidgetEnabled
            settings[skipDirectoriesKey] = settingsData.skipDirectories
        }
    }

    fun getSettingsFlow(): SettingsFlowData{
        val directoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[directoriesKey] ?: DEFAULT_DIRECTORIES
        }
        val tasksTagFlow: Flow<String> = context.dataStore.data.map { preferences ->
            preferences[tasksTagKey] ?: DEFAULT_TASKS_TAG
        }
        val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
            preferences[notificationsEnabledKey] ?: DEFAULT_NOTIFICATIONS_ENABLED
        }
        val dayPlannerNotificationsEnabledFlow : Flow<Boolean> = context.dataStore.data.map { preferences ->
            preferences[dayPlannerNotificationsEnabledKey] ?: DEFAULT_DAY_PLANNER_NOTIFICATIONS_ENABLED
        }
        val updateTimesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[updateTimesKey] ?: DEFAULT_UPDATE_TIMES
        }
        val inProgressTasksEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
            preferences[inProgressTasksEnabledKey] ?: DEFAULT_IN_PROGRESS_TASKS_ENABLED
        }
        val dayPlannerWidgetEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
            preferences[dayPlannerWidgetEnabledKey] ?: DEFAULT_DAY_PLANNER_WIDGET_ENABLED
        }
        val skipDirectoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[skipDirectoriesKey] ?: DEFAULT_SKIP_DIRECTORIES
        }

        return SettingsFlowData(
            directories = directoriesFlow,
            tasksTag = tasksTagFlow,
            notificationsEnabled = notificationsEnabledFlow,
            dayPlannerNotificationsEnabled = dayPlannerNotificationsEnabledFlow,
            updateTimes = updateTimesFlow,
            inProgressTasksEnabled = inProgressTasksEnabledFlow,
            dayPlannerWidgetEnabled = dayPlannerWidgetEnabledFlow,
            skipDirectories = skipDirectoriesFlow
        )
    }

    suspend fun getSettingsMap(settingsFlowData: SettingsFlowData): Map<String, String>{
        val directories = settingsFlowData.directories.firstOrNull()?.joinToString(",") ?: ""
        val tasksTag = settingsFlowData.tasksTag.firstOrNull() ?: DEFAULT_TASKS_TAG
        val notificationsEnabled = settingsFlowData.notificationsEnabled.firstOrNull() ?: DEFAULT_NOTIFICATIONS_ENABLED
        val dayPlannerNotificationsEnabled = settingsFlowData.dayPlannerNotificationsEnabled.firstOrNull() ?: DEFAULT_DAY_PLANNER_NOTIFICATIONS_ENABLED
        val updateTimes = settingsFlowData.updateTimes.firstOrNull()?.joinToString(",") ?: ""
        val inProgressTasksEnabled = settingsFlowData.inProgressTasksEnabled.firstOrNull() ?: DEFAULT_IN_PROGRESS_TASKS_ENABLED
        val dayPlannerWidgetEnabled = settingsFlowData.dayPlannerWidgetEnabled.firstOrNull() ?: DEFAULT_DAY_PLANNER_WIDGET_ENABLED
        val skipDirectories = settingsFlowData.skipDirectories.firstOrNull()?.joinToString(",") ?: ""

        return mapOf(
            directoriesName to directories,
            tasksTagName to tasksTag,
            notificationsEnabledName to notificationsEnabled.toString(),
            dayPlannerNotificationsEnabledName to dayPlannerNotificationsEnabled.toString(),
            updateTimesName to updateTimes,
            inProgressTasksEnabledName to inProgressTasksEnabled.toString(),
            dayPlannerWidgetEnabledName to dayPlannerWidgetEnabled.toString(),
            skipDirectoriesName to skipDirectories
        )
    }

    suspend fun getSettingsData(settingsFlowData: SettingsFlowData): SettingsData{
        val settings = getSettingsMap(settingsFlowData)

        val directories = if(settings[directoriesName] != "")
            settings[directoriesName]!!.split(",").map { Uri.parse(it) }.toSet() else setOf()
        val tasksTag = settings[tasksTagName]!!
        val notificationsEnabled = settings[notificationsEnabledName]!!.toBoolean()
        val dayPlannerNotificationsEnabled = settings[dayPlannerNotificationsEnabledName]!!.toBoolean()
        val updateTimes = if(settings[updateTimesName] != "")
            settings[updateTimesName]!!.split(",").toSet() else setOf()
        val inProgressTasksEnabled = settings[inProgressTasksEnabledName]!!.toBoolean()
        val dayPlannerWidgetEnabled = settings[dayPlannerWidgetEnabledName]!!.toBoolean()
        val skipDirectories = if(settings[skipDirectoriesName] != "")
            settings[skipDirectoriesName]!!.split(",").toSet() else setOf()

        return SettingsData(
            directories = directories,
            tasksTag = tasksTag,
            notificationsEnabled = notificationsEnabled,
            dayPlannerNotificationsEnabled = dayPlannerNotificationsEnabled,
            updateTimes = updateTimes,
            inProgressTasksEnabled = inProgressTasksEnabled,
            dayPlannerWidgetEnabled = dayPlannerWidgetEnabled,
            skipDirectories = skipDirectories
        )
    }


    fun getLastUpdatedFlow(): Flow<String>{
        val flow: Flow<String> = context.dataStore.data.map { preferences ->
            preferences[lastUpdatedKey] ?: DEFAULT_LAST_UPDATED
        }
        return flow
    }

    suspend fun getLastUpdated(flow: Flow<String>): String{
        return flow.firstOrNull().toString()
    }

    suspend fun saveLastUpdated(dateTime: LocalDateTime){
        context.dataStore.edit { settings ->
            settings[lastUpdatedKey] = dateTime.toString() // maybe I should set a formatter to be sure
        }
    }


    fun cancelUpdateTimes(settings: SettingsData, taskAlarmManager: TaskAlarmManager){
        taskAlarmManager.cancelAllUpdateAlarms(settings.updateTimes.size)
    }

    fun setUpdateTimes(settings: SettingsData, taskAlarmManager: TaskAlarmManager){
        taskAlarmManager.createAllUpdateAlarms(settings.updateTimes.toList())
    }

    fun cancelTaskNotifications(tasksSize: Int, taskAlarmManager: TaskAlarmManager){
        taskAlarmManager.cancelAllNotificationAlarmIntent(tasksSize)

    }

    fun setTaskNotifications(tasks: Array<TaskEntityData>, settings: SettingsData, taskAlarmManager: TaskAlarmManager){
        if(settings.notificationsEnabled && settings.dayPlannerNotificationsEnabled){
            taskAlarmManager.createAllNotificationAlarmIntent(tasks.toMutableList(), TaskDisplayManager(settings))
        }else if(settings.notificationsEnabled){
            val tasksList = mutableListOf<TaskEntityData>()
            for(task in tasks){
                if(!task.isDayPlanner.toBoolean()) tasksList.add(task)
            }
            taskAlarmManager.createAllNotificationAlarmIntent(tasksList, TaskDisplayManager(settings))
        }

    }


    suspend fun getTasksArray(settings: SettingsData, date: String): Array<TaskEntityData> = coroutineScope {
        if(settings.directories.isEmpty()){
            return@coroutineScope arrayOf()
        }

        val taskParser = TaskParser(context, settings)
        val tasksDeferredResult = settings.directories.map { directory ->
            async { taskParser.getTasks(LocalDate.parse(date), directory) }
        }

        var uid = 1
        val dbTasks = mutableListOf<TaskEntityData>()

        val tasks = tasksDeferredResult.awaitAll().flatten()

        for(task in tasks){
            // map the Task to the Task Entity Data
            dbTasks.add(TaskEntityData(
                uid = uid,

                task = task.task,
                priority = task.priority.ordinal,
                status = task.status,
                dueDate = task.dueDate,
                scheduledDate = task.scheduledDate,
                startDate = task.startDate,
                evDate = task.evDate,
                evStartTime = task.evStartTime,
                evEndTime = task.evEndTime,
                isDayPlanner = task.isDayPlanner.toString(),
                uri = task.uri.toString()
            ))

            uid++
        }

        dbTasks.toTypedArray()
    }

}