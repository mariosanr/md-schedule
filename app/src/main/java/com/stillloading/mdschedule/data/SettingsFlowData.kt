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