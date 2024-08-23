package com.stillloading.mdschedule.data

import android.content.ContentValues
import android.net.Uri
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract

data class SettingsData(
    val directories: Set<Uri>,
    val tasksTag: String,
    val skipDirectories: Set<String>
)

fun ContentValues.toSettingsData(): SettingsData{
    return SettingsData(
        directories = getAsString(ScheduleProviderContract.SETTINGS.DIRECTORIES).split(",").map {
            Uri.parse(it)
        }.toSet(),
        tasksTag = getAsString(ScheduleProviderContract.SETTINGS.TASKS_TAG),
        skipDirectories = getAsString(ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES).split(",").toSet()
    )
}