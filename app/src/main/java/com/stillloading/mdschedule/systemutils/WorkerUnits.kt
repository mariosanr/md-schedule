package com.stillloading.mdschedule.systemutils

import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.stillloading.mdschedule.notificationsutils.NotificationsCreator
import java.time.LocalDate


const val TAG = "ScheduleWorkerUnits"


// postponed until the tasks are updated from a content observer
class UpdateTasksWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters){


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationsCreator = NotificationsCreator(appContext)
        val (id, notification) = notificationsCreator.getForegroundNotification()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        }catch (_: IllegalStateException){
            return Result.retry()
        }

        val date = inputData.getString("date") ?: return Result.failure()


        // update the tasks database
        val taskUpdateValues = ContentValues().apply {
            put(ScheduleProviderContract.TASKS.DATE, date)
        }
        val responseCode = appContext.contentResolver.update(
            ScheduleProviderContract.TASKS.CONTENT_URI, taskUpdateValues, null, null
        )


        // if it is already updating, return failure
        if(responseCode == ScheduleProviderContract.CODE_UPDATING){
            return Result.failure()
        }
        return Result.success()
    }
}