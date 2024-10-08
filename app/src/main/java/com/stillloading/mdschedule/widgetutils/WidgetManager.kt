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

package com.stillloading.mdschedule.widgetutils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.stillloading.mdschedule.MainActivity
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.TasksWidget
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetManager() {

    companion object {
        const val CLICK_ACTION = "com.stillloading.mdschedule.CLICK"
        const val EXTRA_ITEM = "com.stillloading.mdschedule.EXTRA_ITEM"
    }

    private val TAG = "MarkdownScheduleWidget"


    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
){
        val rootView = RemoteViews(context.packageName, R.layout.widget_main)
        rootView.setTextViewText(R.id.tvWidgetDate, getTodaysDate())

        val lastUpdated = getLastUpdated(context)
        if(lastUpdated != null){
            rootView.setViewVisibility(R.id.tvWidgetLastUpdated, View.VISIBLE)
            rootView.setTextViewText(R.id.tvWidgetLastUpdated, lastUpdated)
        }else{
            rootView.setViewVisibility(R.id.tvWidgetLastUpdated, View.GONE)
        }

        rootView.setViewVisibility(R.id.tvWidgetTasksNone, View.GONE)

        // set on click action to the widget's background (start the app)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rootView.setOnClickPendingIntent(R.id.mainWidgetLayout, openAppPendingIntent)


        /* removed the refresh button on widget until updating is done entirely through work manager
        // set on click action to refresh button
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, TasksWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rootView.setOnClickPendingIntent(R.id.ibRefresh, refreshPendingIntent)
         */


        // set the adapter for the list view to show all the tasks
        val listAdapterIntent = Intent(context, TasksWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME) + System.currentTimeMillis())
        }
        rootView.setRemoteAdapter(R.id.lvWidgetTaskList, listAdapterIntent)
        rootView.setEmptyView(R.id.lvWidgetTaskList, R.id.tvWidgetTasksNone)


        // set the template for the on click action of each of the items in the list view
        val taskClickPendingIntent: PendingIntent = Intent(
            context,
            TasksWidget::class.java
        ).run {
            action = CLICK_ACTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

            PendingIntent.getBroadcast(context, 0, this,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        rootView.setPendingIntentTemplate(R.id.lvWidgetTaskList, taskClickPendingIntent)


        appWidgetManager.updateAppWidget(appWidgetId, rootView)
    }

    private fun getTodaysDate(): String {
        val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, uuuu"))
        return formattedDate
    }

    private fun getLastUpdated(context: Context): String?{
        val contentProviderParser = ContentProviderParser(context = context.applicationContext)
        return contentProviderParser.getLastUpdatedString()
    }


}