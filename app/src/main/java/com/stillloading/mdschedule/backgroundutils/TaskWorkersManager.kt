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

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class TaskWorkersManager(private val appContext: Context) {


    // not in use until periodic updates are fixed
    fun callUpdateTasksWorker(date: String){
        val workManager = WorkManager.getInstance(appContext)

        val updateTasksWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UpdateTasksWorker>()
                .setInputData(
                    workDataOf(
                    "date" to date
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MAX_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        workManager.enqueue(updateTasksWorkRequest)
    }

    // not in use until periodic updates are fixed
    fun callRebootWorkers(){
        val workManager = WorkManager.getInstance(appContext)

        val restartSettingsWorkRequest = OneTimeWorkRequest.from(RestartSettingsWorker::class.java)

        val date = LocalDate.now().toString()

        val updateTasksWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UpdateTasksWorker>()
                .setInputData(
                    workDataOf(
                        "date" to date
                    )
                )
                .build()

        workManager
            .beginUniqueWork(
            "mdschedule_restart_settings_work",
            ExistingWorkPolicy.KEEP,
            restartSettingsWorkRequest
            )
            .then(updateTasksWorkRequest)
            .enqueue()
    }

}