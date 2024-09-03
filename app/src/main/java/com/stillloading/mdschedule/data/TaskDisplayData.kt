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

data class TaskDisplayData(
    val task: String,
    val taskSummary: String,
    val status: String,
    val priority: String,
    val prioritySymbol: String,
    val startDate: String,
    val scheduledDate: String,
    val dueDate: String,
    val evDate: String,
    val evStartTime: String?,
    val evEndTime: String?,
    val evDateTimeString: String,
    val isChecked: Boolean = false,
    val evIsToday: Boolean,
    val priorityNumber: Int,
)
