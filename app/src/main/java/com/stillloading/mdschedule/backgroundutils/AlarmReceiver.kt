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