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
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.stillloading.mdschedule.MainActivity
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.TasksWidget
import com.stillloading.mdschedule.data.TaskWidgetDisplayData
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetManager() {

    companion object {
        const val CLICK_ACTION = "com.stillloading.mdschedule.CLICK"
        const val EXTRA_ITEM = "com.stillloading.mdschedule.EXTRA_ITEM"
        private val tasks: MutableList<TaskWidgetDisplayData> = mutableListOf()
    }

    private val TAG = "MarkdownScheduleWidget"

    private lateinit var contentProviderParser: ContentProviderParser


    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
){
        contentProviderParser = ContentProviderParser(context = context)

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





        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // set the adapter for the list view to show all the tasks
            // update list view in api <=34

            val listAdapterIntent = Intent(context, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME) + System.currentTimeMillis())
            }
            rootView.setRemoteAdapter(R.id.lvWidgetTaskList, listAdapterIntent)
            rootView.setEmptyView(R.id.lvWidgetTaskList, R.id.tvWidgetTasksNone)


            appWidgetManager.updateAppWidget(appWidgetId, rootView)
        } else {
            val itemsBuilder = RemoteViews.RemoteCollectionItems.Builder()

            runBlocking(Dispatchers.IO) {

                contentProviderParser.getWidgetTasks().let { newList ->
                    tasks.clear()
                    tasks.addAll(newList)
                }

            }

            tasks.forEachIndexed{ index, task ->

                val view = RemoteViews(context.packageName, R.layout.widget_item_task)

                view.setTextViewText(R.id.tvWidgetTaskSummary, task.summaryText)
                view.setTextViewText(R.id.tvPriority, task.priority)
                view.setTextViewText(R.id.tvTask, task.task)

                // strike through text that is checked
                if(task.isChecked){
                    view.setTextColor(R.id.tvTask, context.getColor(R.color.checked_text))
                    view.setTextColor(R.id.tvWidgetTaskSummary, context.getColor(R.color.checked_text))
                    view.setInt(R.id.tvTask, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
                }else{
                    view.setTextColor(R.id.tvTask, context.getColor(R.color.unchecked_text))
                    view.setTextColor(R.id.tvWidgetTaskSummary, context.getColor(R.color.unchecked_text))
                    view.setInt(R.id.tvTask, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                }

                val fillInIntent = Intent().apply {
                    Bundle().also { extras ->
                        extras.putInt(WidgetManager.EXTRA_ITEM, index)
                        putExtras(extras)
                    }
                }

                view.setOnClickFillInIntent(R.id.rlWidgetTaskList, fillInIntent)


                itemsBuilder.addItem(index.toLong(), view)
            }

            val items = itemsBuilder.build()

            rootView.setRemoteAdapter(R.id.lvWidgetTaskList, items)

            rootView.setEmptyView(R.id.lvWidgetTaskList, R.id.tvWidgetTasksNone)


            appWidgetManager.updateAppWidget(appWidgetId, rootView)
        }

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