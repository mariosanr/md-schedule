package com.stillloading.mdschedule

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract
import com.stillloading.mdschedule.widgetutils.WidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate


// FIXME the content observer stops receiving the on change updates eventually, seems like no matter what I change
//  I have tried making the content observer, handler and handler thread static with object companion.
//  I have tried setting the observer on update instead of on enabled, and I have removed all user interaction with the widget
class TaskWidgetContentObserver(
    private val appWidgetManager: AppWidgetManager,
    private val componentName: ComponentName,
    handler: Handler?
) : ContentObserver(handler) {

    private val TAG = "WidgetContentObserver"

    override fun onChange(selfChange: Boolean) {
        appWidgetManager.notifyAppWidgetViewDataChanged(
            appWidgetManager.getAppWidgetIds(componentName), R.id.lvWidgetTaskList
        )

    }
}


class TasksWidget : AppWidgetProvider() {

    private val TAG = "WidgetProvider"

    private var contentObserver: TaskWidgetContentObserver? = null
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null

    private var contentProviderParser: ContentProviderParser? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            WidgetManager().updateWidget(context, appWidgetManager, appWidgetId)
        }


        /*
        // update the tasks after updating all the widgets
        CoroutineScope(Dispatchers.IO).launch {
            if(contentProviderParser == null){
                contentProviderParser = ContentProviderParser(context = context.applicationContext)
            }
            contentProviderParser!!.updateTasks(LocalDate.now().toString())
        }
         */
    }

    private fun setContentObserver(context: Context){
        if(contentObserver == null){
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TasksWidget::class.java)

            handlerThread = HandlerThread("TasksWidgetHandlerThread")
            handlerThread?.start()
            handler = handlerThread?.looper?.let { Handler(it) }

            contentObserver = TaskWidgetContentObserver(appWidgetManager, componentName, handler)

            Log.d(TAG, "Set the content observer")
        }

        //
        context.contentResolver.registerContentObserver(
            ScheduleProviderContract.UPDATING_TASKS.CONTENT_URI,
            false,
            contentObserver!!
        )
    }


    override fun onEnabled(context: Context) {
        setContentObserver(context)
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Disabling the content observer")
        contentObserver?.let {
            context.applicationContext.contentResolver.unregisterContentObserver(it)
        }
        handlerThread?.quit()
    }



    override fun onReceive(context: Context, intent: Intent) {
        /* removed because the content observer stops working if you enter the app from clicking the widget. Still dont know the solution
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if(intent.action == WidgetManager.CLICK_ACTION){
            // vars that will probably be needed if I change what the click on each item does
            val appWidgetId: Int = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val viewPosition: Int = intent.getIntExtra(WidgetManager.EXTRA_ITEM, 0)


            // start the app
            val startIntent: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
            startIntent?.let {
                context.startActivity(it)
            }
        }
         */

        super.onReceive(context, intent)
    }

}

