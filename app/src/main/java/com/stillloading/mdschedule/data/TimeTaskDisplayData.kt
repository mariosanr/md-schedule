package com.stillloading.mdschedule.data

data class TimeTaskDisplayData(
    val id: Int,
    var x: Int = 0,
    val y: Int,
    var width: Int = 0,
    val height: Int,
    val overlappingTasks: HashSet<Int> = hashSetOf(),
    var numOverlappingTasks: Int = 1,
    var overlappingOrder: Int = 0,
)
