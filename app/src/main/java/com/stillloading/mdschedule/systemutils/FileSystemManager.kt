package com.stillloading.mdschedule.systemutils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.SettingsFlowData
import com.stillloading.mdschedule.taskutils.TaskParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FileSystemManager(
    private val context: Context
) {

    companion object {
        private val DEFAULT_DIRECTORIES = setOf<String>()
        private val DEFAULT_TASKS_TAG = ""
        private val DEFAULT_SKIP_DIRECTORIES = setOf(".obsidian", ".trash")

        private const val directoriesName = ScheduleProviderContract.SETTINGS.DIRECTORIES
        private const val tasksTagName = ScheduleProviderContract.SETTINGS.TASKS_TAG
        private const val skipDirectoriesName = ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        private val directoriesKey = stringSetPreferencesKey(directoriesName)
        private val tasksTagKey = stringPreferencesKey(tasksTagName)
        private val skipDirectoriesKey = stringSetPreferencesKey(skipDirectoriesName)
    }

    // FIXME Use the Preferences DataStore library instead of SharedPreferences
    suspend fun saveSettings(settingsData: SettingsData) {
        context.dataStore.edit { settings ->
            settings[directoriesKey] = settingsData.directories.map {
                it.toString()
            }.toSet()

            settings[tasksTagKey] = settingsData.tasksTag

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

        val skipDirectoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[skipDirectoriesKey] ?: DEFAULT_SKIP_DIRECTORIES
        }

        return SettingsFlowData(
            directories = directoriesFlow,
            tasksTag = tasksTagFlow,
            skipDirectories = skipDirectoriesFlow
        )
    }

    suspend fun getSettingsMap(settingsFlowData: SettingsFlowData): Map<String, String>{
        val directories = settingsFlowData.directories.firstOrNull()?.joinToString(",") ?: ""
        val tasksTag = settingsFlowData.tasksTag.firstOrNull() ?: ""
        val skipDirectories = settingsFlowData.skipDirectories.firstOrNull()?.joinToString(",") ?: ""

        return mapOf(
            directoriesName to directories,
            tasksTagName to tasksTag,
            skipDirectoriesName to skipDirectories
        )
    }

    suspend fun getSettingsData(settingsFlowData: SettingsFlowData): SettingsData{
        val settings = getSettingsMap(settingsFlowData)

        val directories = if(settings[directoriesName] != "")
            settings[directoriesName]!!.split(",").map { Uri.parse(it) }.toSet() else setOf()
        val tasksTag = settings[tasksTagName]!!
        val skipDirectories = if(settings[skipDirectoriesName] != "")
            settings[skipDirectoriesName]!!.split(",").toSet() else setOf()

        return SettingsData(
            directories = directories,
            tasksTag = tasksTag,
            skipDirectories = skipDirectories
        )
    }

    suspend fun getTasksArray(settings: SettingsData, date: String): Array<TaskEntityData> = coroutineScope {
        if(settings.directories.isEmpty()){
            return@coroutineScope arrayOf()
        }

        val taskParser = TaskParser(context, settings)
        val tasksDeferredResult = settings.directories.map { directory ->
            async { taskParser.getTasks(LocalDate.parse(date), directory) }
        }

        var uid: Int = 1
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
                uri = task.uri.toString()
            ))

            uid++
        }

        dbTasks.toTypedArray()
    }

}