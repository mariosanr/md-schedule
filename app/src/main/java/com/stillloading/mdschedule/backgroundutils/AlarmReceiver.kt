package com.stillloading.mdschedule.backgroundutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stillloading.mdschedule.notificationsutils.NotificationsCreator
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            TaskAlarmManager.UPDATE_TASKS_ACTION -> {
                // call the worker to update
                val date = LocalDate.now().toString()
                TaskWorkersManager(context.applicationContext).callUpdateTasksWorker(date)

                // set the next alarm
                val requestCode = intent.getStringExtra(TaskAlarmManager.UPDATE_TASKS_EXTRA_ID)?.toIntOrNull() ?: return
                val timeString = intent.getStringExtra(TaskAlarmManager.UPDATE_TASKS_EXTRA_TIME) ?: return

                val taskAlarmManager = TaskAlarmManager(context.applicationContext)
                val alarmIntent = taskAlarmManager.getUpdateAlarmIntent(requestCode, timeString)
                taskAlarmManager.setUpdateAlarmIntent(requestCode, timeString, alarmIntent)
            }
            TaskAlarmManager.NOTIFICATIONS_ACTION -> {
                val timeString = intent.getStringExtra(TaskAlarmManager.NOTIFICATIONS_EXTRA_TIME) ?: return
                val task = intent.getStringExtra(TaskAlarmManager.NOTIFICATIONS_EXTRA_TASK) ?: return

                NotificationsCreator(context.applicationContext).createTaskNotification(timeString, task)

            }
        }
    }
}