package com.stillloading.mdschedule.data

import android.net.Uri

data class DirectoryData(
    val uri: Uri?,
    val text: String,
    var position: Int = 0
)