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

        try{
            val date = inputData.getString("date") ?: return Result.failure()

            // update the tasks database and widget
            val contentProviderParser = ContentProviderParser(context = appContext)
            contentProviderParser.updateTasks(date, false) ?: return Result.failure()

            return Result.success()
        }catch (_: Exception){
            return Result.retry()
        }
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
