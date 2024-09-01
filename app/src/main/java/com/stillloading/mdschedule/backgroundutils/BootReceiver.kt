package com.stillloading.mdschedule.backgroundutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {

    companion object{
        const val BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == BOOT_COMPLETED_ACTION){
            TaskWorkersManager(context.applicationContext).callRestartSettingsWorker()
        }
    }
}