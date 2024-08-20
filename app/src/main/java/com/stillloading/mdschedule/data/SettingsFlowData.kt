package com.stillloading.mdschedule.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class SettingsFlowData (
    val directories: Flow<Set<String>>,
    val tasksTag: Flow<String>,
    val skipDirectories: Flow<Set<String>>
)