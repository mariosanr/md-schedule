package com.stillloading.mdschedule.widgetutils

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.stillloading.mdschedule.R
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract

class TasksWidgetService : RemoteViewsService(){
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return TaskRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TaskRemoteViewsFactory(private val context: Context, intent: Intent?)
    : RemoteViewsService.RemoteViewsFactory{

    lateinit var tasks: List<String>

    // TODO implement the reference to the content provider
    override fun onCreate() {
        /*
        context.contentResolver.query(
            ScheduleProviderContract.TASKS.CONTENT_URI,
            null,
            null,
            null,
            null
        )
         */

        tasks = listOf("1", "2", "3")
    }

    override fun onDataSetChanged() {}

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    // TODO open the app with the popup of the task on click
    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_item_task)

        //view.setTextViewText(R.id.tvWidgetTaskSummary, tasks[position].summaryText)
        //view.setTextViewText(R.id.tvPriority, tasks[position].priority)
        view.setTextViewText(R.id.tvTask, tasks[position])

        return view
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

}