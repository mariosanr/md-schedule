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

import kotlinx.coroutines.flow.Flow

data class SettingsFlowData (
    val directories: Flow<Set<String>>,
    val tasksTag: Flow<String>,
    val notificationsEnabled: Flow<Boolean>,
    val dayPlannerNotificationsEnabled: Flow<Boolean>,
    val updateTimes: Flow<Set<String>>,
    val inProgressTasksEnabled: Flow<Boolean>,
    val dayPlannerWidgetEnabled: Flow<Boolean>,
    val skipDirectories: Flow<Set<String>>
)