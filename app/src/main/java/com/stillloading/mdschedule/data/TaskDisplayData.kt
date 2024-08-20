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
