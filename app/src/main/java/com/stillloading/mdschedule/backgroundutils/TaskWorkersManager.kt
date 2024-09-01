package com.stillloading.mdschedule.backgroundutils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class TaskWorkersManager(private val appContext: Context) {


    fun callUpdateTasksWorker(date: String){
        val workManager = WorkManager.getInstance(appContext)

        val updateTasksWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UpdateTasksWorker>()
                .setInputData(
                    workDataOf(
                    "date" to date
                    )
                )
                //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MAX_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        workManager.enqueue(updateTasksWorkRequest)
    }

    fun callRestartSettingsWorker(){
        val workManager = WorkManager.getInstance(appContext)

        val restartSettingsWorkRequest = OneTimeWorkRequest.from(RestartSettingsWorker::class.java)

        workManager.enqueueUniqueWork(
            "mdschedule_restart_settings_work",
            ExistingWorkPolicy.KEEP,
            restartSettingsWorkRequest
        )
    }

}