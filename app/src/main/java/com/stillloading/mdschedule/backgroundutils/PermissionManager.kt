package com.stillloading.mdschedule.backgroundutils

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(private val appContext: Context) {

    fun hasNotificationsPermission(): Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS)
            return permissionState == PackageManager.PERMISSION_GRANTED
        }else{
            return true
        }
    }


    fun hasExactAlarmPermission(alarmManager: AlarmManager?): Boolean{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

}