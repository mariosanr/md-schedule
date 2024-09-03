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

import android.net.Uri

enum class TaskPriority{
    LOWEST, LOW, NORMAL, MEDIUM, HIGH, HIGHEST
}

data class Task (
    val task: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    var status: String? = null,
    val dueDate: String? = null,
    val scheduledDate: String? = null,
    val startDate: String? = null,
    val evDate: String? = null,
    val evStartTime: String? = null,
    val evEndTime: String? = null,
    val isDayPlanner: Boolean = false,
    val uri: Uri? = null,
)

data class TaskDates (
    var dueDate: String? = null,
    var scheduledDate: String? = null,
    var startDate: String? = null,
    var evDate: String? = null,
    var evStartTime: String? = null,
    var evEndTime: String? = null
)

