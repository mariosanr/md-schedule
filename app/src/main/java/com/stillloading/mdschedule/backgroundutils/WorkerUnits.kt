package com.stillloading.mdschedule.backgroundutils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stillloading.mdschedule.systemutils.ContentProviderParser


const val TAG = "ScheduleWorkerUnits"


class UpdateTasksWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters){


    /*
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
     */

    override suspend fun doWork(): Result {
        /* for if I do expedited work
        try {
            setForeground(getForegroundInfo())
        }catch (_: IllegalStateException){
            return Result.retry()
        }
         */

        val date = inputData.getString("date") ?: return Result.failure()

        // update the tasks database
        val contentProviderParser = ContentProviderParser(context = appContext)
        contentProviderParser.updateTasks(date, false) ?: return Result.failure()

        return Result.success()
    }
}


class RestartSettingsWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters){

    override suspend fun doWork(): Result {
        val contentProviderParser = ContentProviderParser(context = appContext)
        contentProviderParser.restartSettings()

        return Result.success()
    }

}
