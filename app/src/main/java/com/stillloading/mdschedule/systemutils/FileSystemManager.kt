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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import java.time.LocalDate

class FileSystemManager(
    private val context: Context
) {

    companion object {
        private val defaultSettings = SettingsData(
            directories = setOf(),
            tasksTag = "",
            skipDirectories = setOf(".obsidian", ".trash")
        )

        private const val directoriesName = "directories"
        private const val tasksTagName = "tasks_tag"
        private const val skipDirectoriesName = "skip_directories"

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        private val directoriesKey = stringSetPreferencesKey(directoriesName)
        private val tasksTagKey = stringPreferencesKey(tasksTagName)
        private val skipDirectoriesKey = stringSetPreferencesKey(skipDirectoriesName)
    }

    // FIXME Use the Preferences DataStore library instead of SharedPreferences
    suspend fun saveSettings(settingsArg: SettingsData?) {
        val settingsData = settingsArg ?: defaultSettings

        context.dataStore.edit { settings ->
            settings[directoriesKey] = settingsData.directories.map {
                it.toString()
            }.toSet()

            settings[tasksTagKey] = settingsData.tasksTag

            settings[skipDirectoriesKey] = settingsData.skipDirectories
        }
    }

        /*
        val settings = settingsArg ?: defaultSettings
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        val directories = java.util.HashSet<String>()
        val skipDirectories = java.util.HashSet<String>()

        for(directory in settings.directories){
            directories.add(directory.toString())
        }
        for(directory in settings.skipDirectories){
            skipDirectories.add(directory)
        }


        editor.putStringSet("directories", directories)
        editor.putString("tasks_tag", settings.tasksTag)
        editor.putStringSet("skip_directories", skipDirectories)

        editor.apply()
    }
         */

    fun getSettingsFlow(): SettingsFlowData{
        val directoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[directoriesKey] ?: setOf()
        }

        val tasksTagFlow: Flow<String> = context.dataStore.data.map { preferences ->
            preferences[tasksTagKey] ?: ""
        }

        val skipDirectoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
            preferences[skipDirectoriesKey] ?: setOf()
        }

        return SettingsFlowData(
            directories = directoriesFlow,
            tasksTag = tasksTagFlow,
            skipDirectories = skipDirectoriesFlow
        )
    }


        /*
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)


        val directories = mutableListOf<Uri>()
        val skipDirectories = mutableListOf<String>()

        for(directory in sharedPreferences.getStringSet("directories", java.util.HashSet())!!){
            directories.add(Uri.parse(directory))
        }
        for(directory in sharedPreferences.getStringSet("skip_directories", java.util.HashSet())!!){
            skipDirectories.add(directory)
        }

        return SettingsData(
            directories = directories,
            tasksTag = sharedPreferences.getString("tasks_tag", "") ?: "",
            skipDirectories = skipDirectories
        )
    }
         */

    suspend fun getSettings(settingsFlowData: SettingsFlowData): Map<String, String>{
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
        val settings = getSettings(settingsFlowData)

        val directories = settings[directoriesName]!!.split(",").map {
            Uri.parse(it)
        }.toSet()
        val tasksTag = settings[tasksTagName]!!
        val skipDirectories = settings[skipDirectoriesName]!!.split(",").toSet()

        return SettingsData(
            directories = directories,
            tasksTag = tasksTag,
            skipDirectories = skipDirectories
        )
    }

    suspend fun getTasksArray(settings: SettingsData, date: String): Array<TaskEntityData> = coroutineScope {
        val taskParser = TaskParser(context, settings)
        val tasksDeferredResult = settings.directories.map { directory ->
            async { taskParser.getTasks(LocalDate.parse(date), directory) }
        }

        var uid: Int = 1
        val dbTasks = mutableListOf<TaskEntityData>()

        val tasks = tasksDeferredResult.awaitAll().flatten()
        Log.d("list of tasks", "Num of Tasks: ${tasks.size}\nTasks: $tasks")

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