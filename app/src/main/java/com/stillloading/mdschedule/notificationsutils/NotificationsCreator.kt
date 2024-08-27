package com.stillloading.mdschedule.notificationsutils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.stillloading.mdschedule.R

class NotificationsCreator(private val appContext: Context) {

    companion object{

        object ForegroundService{

            object Channel{
                const val ID = "foreground_service_notifs"
                const val NAME = "Foreground Services"
                const val DESCRIPTION = "Notifies if the tasks are updating"
            }

            const val NOTIFICATION_ID = 0
            const val CONTENT_TITLE = "Markdown Schedule is updating"
        }
    }

    fun getNotificationPermission(){
    }

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
}