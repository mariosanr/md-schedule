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
    val uri: Uri? = null,
)

data class UnParsedTask(
    val fileTitle: String?,
    val task: String,
    val uri: Uri
)