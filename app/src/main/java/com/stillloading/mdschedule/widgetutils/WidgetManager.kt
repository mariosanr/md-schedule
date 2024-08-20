package com.stillloading.mdschedule.widgetutils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.stillloading.mdschedule.MainActivity
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.TasksWidget
import com.stillloading.mdschedule.systemutils.ScheduleContentProvider
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetManager() {

    private val TAG = "MarkdownScheduleWidget"

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
){
        Log.i(TAG, "Updating Widget")
        val rootView = RemoteViews(context.packageName, R.layout.widget_main)
        rootView.setTextViewText(R.id.tvWidgetDate, getTodaysDate())

        rootView.setViewVisibility(R.id.tvWidgetTasksNone, View.GONE)


        val listAdapterIntent = Intent(context, TasksWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        rootView.setRemoteAdapter(R.id.lvWidgetTaskList, listAdapterIntent)
        rootView.setEmptyView(R.id.lvWidgetTaskList, R.id.tvWidgetTasksNone)


        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rootView.setOnClickPendingIntent(R.id.mainWidgetLayout, openAppPendingIntent)


        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, TasksWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        rootView.setOnClickPendingIntent(R.id.ibRefresh, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, rootView)
    }


    private fun getTodaysDate(): String {
        val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE: MMM d, uuuu"))
        return formattedDate
    }
}