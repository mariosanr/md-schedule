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

package com.stillloading.mdschedule.backgroundutils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stillloading.mdschedule.systemutils.TaskEntityData
import com.stillloading.mdschedule.taskutils.TaskDisplayManager
import java.time.LocalTime
import java.util.Calendar

class TaskAlarmManager(private val appContext: Context) {

    val TAG = "TaskAlarmManager"

    companion object{
        const val UPDATE_TASKS_ACTION = "com.stillloading.mdschedule.alarms.UPDATE_TASKS"
        const val UPDATE_TASKS_EXTRA_ID = "com.stillloading.mdschedule.alarms.EXTRA_ID"
        const val UPDATE_TASKS_EXTRA_TIME = "com.stillloading.mdschedule.alarms.EXTRA_TIME"

        const val NOTIFICATIONS_ACTION = "com.stillloading.mdschedule.alarms.NOTIFICATIONS"
        const val NOTIFICATIONS_EXTRA_TIME = "com.stillloading.mdschedule.alarms.EXTRA_TIME"
        const val NOTIFICATIONS_EXTRA_TASK = "com.stillloading.mdschedule.alarms.EXTRA_TASK"
    }

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val permissionManager = PermissionManager(appContext)

    private val getTimeRegEx = Regex("^(?<hour>\\d\\d?):(?<minutes>\\d{2})")

    // Alarms to update the tasks database

    private fun getUpdateAlarmIntent(): Intent{
        val alarmIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = UPDATE_TASKS_ACTION
        }

        return alarmIntent
    }

    fun getUpdateAlarmIntent(requestCode: Int, timeString: String): Intent{
        val alarmIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = UPDATE_TASKS_ACTION

            putExtra(UPDATE_TASKS_EXTRA_ID, requestCode.toString())
            putExtra(UPDATE_TASKS_EXTRA_TIME, timeString)
        }

        return alarmIntent
    }

    fun setUpdateAlarmIntent(requestCode: Int, timeString: String, intent: Intent){
        val hour = timeString.substring(0, 2).toIntOrNull() ?: return
        val minutes = timeString.substring(3, 5).toIntOrNull() ?: return

        val currentTime = LocalTime.now()
        val alarmTime = LocalTime.of(hour, minutes)

        val hasPassed = currentTime.isAfter(alarmTime) || currentTime.equals(alarmTime)

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minutes)

            if(hasPassed){
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
           PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

    }

    fun cancelAllUpdateAlarms(updateTimesCount: Int){
        val intent = getUpdateAlarmIntent()
        for(i in 0..<updateTimesCount){
            cancelAlarm(i, intent)
        }
    }


    fun createAllUpdateAlarms(updateTimes: List<String>){
        for(i in 0..<updateTimes.size){
            val intent = getUpdateAlarmIntent(i, updateTimes[i])
            setUpdateAlarmIntent(i, updateTimes[i], intent)
        }
    }



    // Alarms to send notifications of event tasks

    private fun getNotificationAlarmIntent(): Intent{
        val notificationIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = NOTIFICATIONS_ACTION
        }

        return notificationIntent

    }

    private fun getNotificationAlarmIntent(taskEntityData: TaskEntityData, taskDisplayManager: TaskDisplayManager): Intent{
        val notificationIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = NOTIFICATIONS_ACTION

            if(taskEntityData.evEndTime != null){
                putExtra(NOTIFICATIONS_EXTRA_TIME, "${taskEntityData.evStartTime} - ${taskEntityData.evEndTime}")
            }else{
                putExtra(NOTIFICATIONS_EXTRA_TIME, taskEntityData.evStartTime)
            }

            putExtra(NOTIFICATIONS_EXTRA_TASK, taskDisplayManager.getParsedTaskText(taskEntityData.task))
        }

        return notificationIntent
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setNotificationAlarmIntent(requestCode: Int, taskEntityData: TaskEntityData, intent: Intent){
        val match = taskEntityData.evStartTime?.let { getTimeRegEx.find(it) } ?: return
        val hour = match.groups.get("hour")?.value?.toIntOrNull() ?: return
        val minutes = match.groups.get("minutes")?.value?.toIntOrNull() ?: return

        // dont set the alarm if the time has already passed
        val currentTime = LocalTime.now()
        val alarmTime = LocalTime.of(hour, minutes)
        if(currentTime.isAfter(alarmTime)) return


        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        if(permissionManager.hasExactAlarmPermission(alarmManager)){
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }else{
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }


    }

    fun cancelAllNotificationAlarmIntent(tasksCount: Int){
        val intent = getNotificationAlarmIntent()
        for(i in 0..tasksCount + 10){ // add 10 as an extra measure to make sure they are all cancelled
            cancelAlarm(i, intent)
        }

    }

    fun createAllNotificationAlarmIntent(tasks: MutableList<TaskEntityData>, taskDisplayManager: TaskDisplayManager){
        for(i in 0..<tasks.size){
            if(tasks[i].evStartTime != null){
                val intent = getNotificationAlarmIntent(tasks[i], taskDisplayManager)
                setNotificationAlarmIntent(i, tasks[i], intent)
            }
        }

    }


    // For use of both alarm types

    private fun cancelAlarm(requestCode: Int, intent: Intent){
        val pendingIntent = PendingIntent.getBroadcast(appContext, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        if(pendingIntent != null && alarmManager != null){
            alarmManager.cancel(pendingIntent)
        }
    }
}