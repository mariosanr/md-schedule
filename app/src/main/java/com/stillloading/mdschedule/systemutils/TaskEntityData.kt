package com.stillloading.mdschedule.systemutils

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stillloading.mdschedule.data.TaskPriority

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
    @ColumnInfo(name = "uri") val uri: String?,
)

