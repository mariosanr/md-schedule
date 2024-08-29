package com.stillloading.mdschedule.data

import android.content.ContentValues
import android.net.Uri
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract

data class SettingsData(
    val directories: Set<Uri>,
    val tasksTag: String,
    val notificationsEnabled: Boolean,
    val dayPlannerNotificationsEnabled: Boolean,
    val updateTimes: Set<String>,
    val inProgressTasksEnabled: Boolean,
    val dayPlannerWidgetEnabled: Boolean,
    val skipDirectories: Set<String>
)

fun ContentValues.toSettingsData(): SettingsData{
    return SettingsData(
        directories = getAsString(ScheduleProviderContract.SETTINGS.DIRECTORIES).split(",").map {
            Uri.parse(it)
        }.toSet(),
        tasksTag = getAsString(ScheduleProviderContract.SETTINGS.TASKS_TAG),
        notificationsEnabled = getAsBoolean(ScheduleProviderContract.SETTINGS.NOTIFICATIONS_ENABLED),
        dayPlannerNotificationsEnabled = getAsBoolean(ScheduleProviderContract.SETTINGS.DAY_PLANNER_NOTIFICATIONS_ENABLED),
        updateTimes = getAsString(ScheduleProviderContract.SETTINGS.UPDATE_TIMES).split(",").toSet(),
        inProgressTasksEnabled = getAsBoolean(ScheduleProviderContract.SETTINGS.IN_PROGRESS_TASKS_ENABLED),
        dayPlannerWidgetEnabled = getAsBoolean(ScheduleProviderContract.SETTINGS.DAY_PLANNER_WIDGET_ENABLED),
        skipDirectories = getAsString(ScheduleProviderContract.SETTINGS.SKIP_DIRECTORIES).split(",").toSet()
    )
}