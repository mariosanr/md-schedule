package com.stillloading.mdschedule.data

data class SettingsDisplayData(
    val directories: MutableList<DirectoryData>,
    val tasksTag: String,
    val skipDirectories: MutableList<String>
)
