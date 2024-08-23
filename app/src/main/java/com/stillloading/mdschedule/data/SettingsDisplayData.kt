package com.stillloading.mdschedule.data

import android.content.ContentValues
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract

data class SettingsDisplayData(
    var directories: MutableList<DirectoryData> = mutableListOf(),
    var tasksTag: String = "",
    var skipDirectories: MutableList<String> = mutableListOf()
)

fun SettingsDisplayData.toSettingsData(): SettingsData{
    return SettingsData(
        directories = directories.mapNotNull { it.uri }.toSet(),
        tasksTag = tasksTag,
        skipDirectories = skipDirectories.toSet()
    )
}

fun SettingsDisplayData.toContentValues(): ContentValues {
    return ContentValues().apply {
        put(ScheduleProviderContract.SETTINGS.DIRECTORIES, directories.joinToString(",") {
            it.uri.toString()
        })
        put(ScheduleProviderContract.SETTINGS.TASKS_TAG, tasksTag)
        put(ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES, skipDirectories.joinToString(","))
    }
}
