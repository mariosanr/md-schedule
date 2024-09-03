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

package com.stillloading.mdschedule.systemutils

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntityData(
    @PrimaryKey val uid: Int,

    @ColumnInfo(name = "task") val task: String,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "status") val status: String?,
    @ColumnInfo(name = "due_date") val dueDate: String?,
    @ColumnInfo(name = "scheduled_date") val scheduledDate: String?,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "ev_date") val evDate: String?,
    @ColumnInfo(name = "ev_start_time") val evStartTime: String?,
    @ColumnInfo(name = "ev_end_time") val evEndTime: String?,
    @ColumnInfo(name = "is_day_planner") val isDayPlanner: String,
    @ColumnInfo(name = "uri") val uri: String?,
)

