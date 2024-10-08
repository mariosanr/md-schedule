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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.data.TaskWidgetDisplayData
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TasksWidgetService : RemoteViewsService(){
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return TaskRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TaskRemoteViewsFactory(private val context: Context, intent: Intent?)
    : RemoteViewsService.RemoteViewsFactory{

    private val TAG = "WidgetListAdapter"
    private val tasks: MutableList<TaskWidgetDisplayData> = mutableListOf()

    private var contentProviderParser: ContentProviderParser = ContentProviderParser(context = context) // already is appContext

    private var widgetId: Int = 0

    init {
        widgetId = intent!!.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking(Dispatchers.Main) {

            contentProviderParser.getWidgetTasks().let { newList ->
                tasks.clear()
                tasks.addAll(newList)
            }

            // update the tasks from the content observer (does loading wheel when in process)
            /*
            val isUpdatingTasks = contentProviderParser.getIsUpdatingTasks()

            // make references to get the widget remote view
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val remoteView = RemoteViews(context.packageName, R.layout.widget_main)

            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE: MMM d, uuuu"))

            if(isUpdatingTasks){
                Log.d(TAG, "Task is updating")
                remoteView.also {
                    it.setTextViewText(R.id.tvWidgetDate, date)
                    it.setViewVisibility(R.id.pbWidgetLoadingWheel, View.VISIBLE)
                }
            }else{
                Log.d(TAG, "Task is finished updating")
                remoteView.also {
                    it.setTextViewText(R.id.tvWidgetDate, date)
                    it.setViewVisibility(R.id.pbWidgetLoadingWheel, View.GONE)
                }
            }

            contentProviderParser.getWidgetTasks().let { newList ->
                tasks.clear()
                tasks.addAll(newList)
            }

            Log.d(TAG, "Partially updating widget")
            appWidgetManager.partiallyUpdateAppWidget(widgetId, remoteView)
             */

        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size


    override fun getViewAt(position: Int): RemoteViews {
        if(count == 0){
            tasks.add(TaskWidgetDisplayData(
                task = "Error. PLease refresh or delete widget if the error persists",
                priority = "",
                summaryText = "",
                isChecked = false
            ))
        }


        val view = RemoteViews(context.packageName, R.layout.widget_item_task)

        view.setTextViewText(R.id.tvWidgetTaskSummary, tasks[position].summaryText)
        view.setTextViewText(R.id.tvPriority, tasks[position].priority)
        view.setTextViewText(R.id.tvTask, tasks[position].task)

        // strike through text that is checked
        if(tasks[position].isChecked){
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