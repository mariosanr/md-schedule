package com.stillloading.mdschedule.data

import android.content.ContentValues
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract

data class SettingsDisplayData(
    var directories: MutableList<DirectoryData> = mutableListOf(),
    var tasksTag: String = "",
    var notificationsEnabled: Boolean = false,
    var dayPlannerNotificationsEnabled: Boolean = false,
    var updateTimes: MutableList<UpdateTimesData> = mutableListOf(),
    var inProgressTasksEnabled: Boolean = false,
    var dayPlannerWidgetEnabled: Boolean = false,
    var skipDirectories: MutableList<SkipDirectoryData> = mutableListOf()
)

fun SettingsDisplayData.toSettingsData(): SettingsData{
    return SettingsData(
        directories = directories.mapNotNull { it.uri }.toSet(),
        tasksTag = tasksTag,
        notificationsEnabled = notificationsEnabled,
        dayPlannerNotificationsEnabled = dayPlannerNotificationsEnabled,
        updateTimes = updateTimes.map { it.timeString }.toSet(),
        inProgressTasksEnabled = inProgressTasksEnabled,
        dayPlannerWidgetEnabled = dayPlannerWidgetEnabled,
        skipDirectories = skipDirectories.map { it.text }.toSet()
    )
}

fun SettingsDisplayData.toContentValues(): ContentValues {
    return ContentValues().apply {
        put(ScheduleProviderContract.SETTINGS.DIRECTORIES, directories.joinToString(",") {
            it.uri.toString()
        })
        put(ScheduleProviderContract.SETTINGS.TASKS_TAG, tasksTag)
        put(ScheduleProviderContract.SETTINGS.NOTIFICATIONS_ENABLED, notificationsEnabled)
        put(ScheduleProviderContract.SETTINGS.DAY_PLANNER_NOTIFICATIONS_ENABLED, dayPlannerNotificationsEnabled)
        put(ScheduleProviderContract.SETTINGS.UPDATE_TIMES, updateTimes.joinToString(","){
            it.timeString
        })
        put(ScheduleProviderContract.SETTINGS.IN_PROGRESS_TASKS_ENABLED, inProgressTasksEnabled)
        put(ScheduleProviderContract.SETTINGS.DAY_PLANNER_WIDGET_ENABLED, dayPlannerWidgetEnabled)
        put(ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES, skipDirectories.joinToString(","){
            it.text
        })
    }
}
