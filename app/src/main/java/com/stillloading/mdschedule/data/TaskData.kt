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

