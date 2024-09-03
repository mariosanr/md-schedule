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

package com.stillloading.mdschedule.notificationsutils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stillloading.mdschedule.MainActivity
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.backgroundutils.PermissionManager
import kotlin.random.Random

class NotificationsCreator(private val appContext: Context) {

    companion object{

        object Tasks{
            object Channel{
                const val ID = "com.stillloading.mdschedule.notifications.tasks"
                const val NAME = "Tasks"
                const val DESCRIPTION = "Get notifications from tasks with a set time"
            }
        }

        /*
        object ForegroundService{

            object Channel{
                const val ID = "com.stillloading.mdschedule.notifications.foreground_services"
                const val NAME = "Foreground Services"
                const val DESCRIPTION = "Notifies if the tasks are updating"
            }

            const val NOTIFICATION_ID = 0
            const val CONTENT_TITLE = "Markdown Schedule is updating"
        }
         */
    }

    private val permissionManager = PermissionManager(appContext)


    fun createTasksNotificationsChannel(){
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            Tasks.Channel.ID,
            Tasks.Channel.NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = Tasks.Channel.DESCRIPTION
        channel.enableLights(true)
        channel.lightColor = appContext.getColor(R.color.purple)

        notificationManager.createNotificationChannel(channel)
    }

    fun createTaskNotification(timeString: String, task: String){
        if(permissionManager.hasNotificationsPermission()){
            val intent = Intent(appContext, MainActivity::class.java)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val notificationBuilder = NotificationCompat.Builder(appContext, Tasks.Channel.ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(appContext.getColor(R.color.purple))
                .setContentTitle(timeString)
                .setContentText(task)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationId = Random.nextInt()
            val notificationTag = System.currentTimeMillis().toString()

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
        }
    }




    /*
    fun createForegroundNotificationChannel(){
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            ForegroundService.Channel.ID,
            ForegroundService.Channel.NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = ForegroundService.Channel.DESCRIPTION
        channel.enableLights(true)
        channel.lightColor = appContext.getColor(R.color.purple)

        notificationManager.createNotificationChannel(channel)
    }

    fun getForegroundNotification(): Pair<Int, Notification>{
        // move to onStart
        createForegroundNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(appContext, ForegroundService.Channel.ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(ForegroundService.CONTENT_TITLE)
            .setOngoing(true)
            .setAutoCancel(true)

        return Pair(ForegroundService.NOTIFICATION_ID, notificationBuilder.build())
    }
     */

}