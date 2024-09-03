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