package com.stillloading.mdschedule.widgetutils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.IntentCompat
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.data.TaskWidgetDisplayData
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.properties.Delegates

class TasksWidgetService : RemoteViewsService(){
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return TaskRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TaskRemoteViewsFactory(private val context: Context, intent: Intent?)
    : RemoteViewsService.RemoteViewsFactory{

    private val TAG = "MarkdownScheduleWidget"
    private var tasks: List<TaskWidgetDisplayData> = listOf()

    private var contentProviderParser: ContentProviderParser = ContentProviderParser(context = context) // already is appContext

    private var widgetId: Int = 0

    init {
        widgetId = intent!!.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking(Dispatchers.Main) {
            Log.i(TAG, "On Data Set Changed")

            // TODO toggle the widget loading wheel
            val isUpdatingTasks = contentProviderParser.getIsUpdatingTasks()

            // make references to get the widget remote view
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val remoteView = RemoteViews(context.packageName, R.layout.widget_main)

            if(isUpdatingTasks){
                remoteView.also {
                    it.setViewVisibility(R.id.pbWidgetLoadingWheel, View.VISIBLE)
                }
            }else{
                remoteView.also {
                    it.setViewVisibility(R.id.pbWidgetLoadingWheel, View.GONE)
                }
            }

            tasks = contentProviderParser.getWidgetTasks(update = false) ?: tasks

            appWidgetManager.partiallyUpdateAppWidget(widgetId, remoteView)

        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    // TODO MAYBE open the app with the popup of the task on click
    override fun getViewAt(position: Int): RemoteViews {
        if(count == 0){
            tasks = listOf(TaskWidgetDisplayData(
                task = "Error. PLease refresh or delete widget if the error persists",
                priority = "",
                summaryText = ""
            ))
        }

        val view = RemoteViews(context.packageName, R.layout.widget_item_task)

        view.setTextViewText(R.id.tvWidgetTaskSummary, tasks[position].summaryText)
        view.setTextViewText(R.id.tvPriority, tasks[position].priority)
        view.setTextViewText(R.id.tvTask, tasks[position].task)

        val fillInIntent = Intent().apply {
            Bundle().also { extras ->
                extras.putInt(WidgetManager.EXTRA_ITEM, position)
                putExtras(extras)
            }
        }

        view.setOnClickFillInIntent(R.id.rlWidgetTaskList, fillInIntent)

        return view
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

}