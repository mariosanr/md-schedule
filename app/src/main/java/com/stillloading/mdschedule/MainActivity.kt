package com.stillloading.mdschedule

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.database.getStringOrNull
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.Task
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.data.TaskPriority
import com.stillloading.mdschedule.data.toContentValues
import com.stillloading.mdschedule.databinding.PopupTaskBinding
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import com.stillloading.mdschedule.systemutils.FileSystemManager
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract
import com.stillloading.mdschedule.taskutils.DayViewHourAdapter
import com.stillloading.mdschedule.taskutils.NonTimeTaskAdapter
import com.stillloading.mdschedule.taskutils.TaskDisplayManager
import com.stillloading.mdschedule.taskutils.TaskPopup
import com.stillloading.mdschedule.taskutils.TimeTaskManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONTokener
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val TAG = "Md Companion Debug"

    private lateinit var nonTimeTaskAdapter: NonTimeTaskAdapter
    private lateinit var dayViewHourAdapter: DayViewHourAdapter
    private lateinit var timeTaskManager: TimeTaskManager
    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var contentProviderParser: ContentProviderParser

    private val settingFilename = "paths_settings"
    private var directoryList: MutableList<DirectoryData> = mutableListOf()

    private lateinit var tvDate: TextView
    private lateinit var linearLayoutMain: LinearLayout
    private lateinit var rvTaskList: RecyclerView
    private lateinit var rvHourList: RecyclerView
    private lateinit var frameTimeContainer: FrameLayout
    private lateinit var pbLoadingWheel: ProgressBar

    private lateinit var tvTimeTasksNone: TextView
    private lateinit var tvNonTimeTasksNone: TextView

    private var minHour: Int = -1
    private var maxHour: Int = -1

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvDate = findViewById(R.id.tvDate)
        linearLayoutMain = findViewById(R.id.linearLayoutMain)
        tvTimeTasksNone = findViewById(R.id.tvTimeTasksNone)
        tvNonTimeTasksNone = findViewById(R.id.tvNonTimeTasksNone)


        setTodayDate()

        fileSystemManager = FileSystemManager(applicationContext)
        contentProviderParser = ContentProviderParser(applicationContext)

        /*
        val popupInflater: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = popupInflater.inflate(R.layout.popup_task, null)
        val taskPopup = TaskPopup(popupView, this)
         */
        val popupBinding = PopupTaskBinding.inflate(layoutInflater)
        val taskPopup = TaskPopup(popupBinding, this)

        // TODO get the task list from a saved file probably
        val taskLists: MutableList<TaskDisplayData> = mutableListOf()
        nonTimeTaskAdapter = NonTimeTaskAdapter(taskLists, taskPopup, this)

        rvTaskList = findViewById(R.id.rvTaskList)
        rvTaskList.adapter = nonTimeTaskAdapter
        rvTaskList.layoutManager = LinearLayoutManager(this)

        // add the divide between the tasks in the first recycler view
        AppCompatResources.getDrawable(this, R.drawable.rv_divider)?.let {
            val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
            divider.setDrawable(it)
            rvTaskList.addItemDecoration(divider)
        }

        frameTimeContainer = findViewById(R.id.frameTimeContainer)

        val frameLayout: FrameLayout = findViewById(R.id.frameTimeTasks)
        timeTaskManager = TimeTaskManager(this, frameTimeContainer, frameLayout, layoutInflater, taskPopup)

        dayViewHourAdapter = DayViewHourAdapter(8, 24)
        rvHourList = findViewById(R.id.rvHourList)
        rvHourList.adapter = dayViewHourAdapter
        rvHourList.layoutManager = LinearLayoutManager(this)


        pbLoadingWheel = findViewById(R.id.pbLoadingWheel)


        reloadTasks()
    }

    override fun onStart() {
        super.onStart()

        if(timeTaskManager.timeBarView != null){
            timeTaskManager.setTimeBar(minHour, maxHour)
        }
    }

    private fun setTodayDate(){
        val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, uuuu"))
        val tvDateMessage = "Today: $formattedDate"
        tvDate.text = tvDateMessage
    }

    private fun loadSettings(){
        directoryList = mutableListOf()

        val file = File(applicationContext.filesDir, settingFilename)
        if(!file.exists()) return

        val directoryListString = file.readText(Charsets.UTF_8)
        val directoryJSONArray = JSONTokener(directoryListString).nextValue()

        if(directoryJSONArray is JSONArray){
            for(i in 0..<directoryJSONArray.length()){
                val dataObject = directoryJSONArray.getJSONObject(i)
                directoryList.add(DirectoryData(Uri.parse(dataObject.getString("uri")), dataObject.getString("path"), dataObject.getInt("position")))
            }
        }

    }

    private fun setMinMaxHours(tasksList: MutableList<TaskDisplayData>){
        val hourRegEx = Regex("^(?<hour>\\d\\d?)")
        minHour = -1
        maxHour = -1

        for(task in tasksList){
            if(task.evStartTime == null) continue
            var startTime = hourRegEx.find(task.evStartTime)?.groups?.get("hour")?.value?.toInt()
            if (startTime != null) {
                startTime = if(startTime >= 24) 24 else startTime
                if((minHour == -1) or (startTime < minHour)){
                    minHour = startTime
                }

                var endTime: Int = startTime + 1
                if(task.evEndTime != null) {
                    endTime = hourRegEx.find(task.evEndTime)?.groups?.get("hour")?.value?.toInt() ?: endTime
                }

                endTime = if(endTime + 1 > 24) 24 else endTime + 1
                if((maxHour == -1) or (endTime > maxHour)){
                    maxHour = endTime
                }
            }

        }

        if (minHour == -1){
            minHour = 8
        }
        if(maxHour == -1){
            maxHour = minHour + 1
        }
    }


    // Return Pair(timeTasks, nonTimeTasks)
    private suspend fun getTasks(uris: Set<Uri>): Pair<MutableList<TaskDisplayData>, MutableList<TaskDisplayData>> {
        return withContext(Dispatchers.IO){

            val settings = SettingsData(
                directories = uris,
                tasksTag = "#todo",
                skipDirectories = setOf(".obsidian", ".trash")
            )


            val updateSettingsResult = applicationContext.contentResolver.update(
                ScheduleProviderContract.SETTINGS.CONTENT_URI, settings.toContentValues(), null, null
            )
            Log.d(TAG, "Update Settings Result: $updateSettingsResult")


            val today = LocalDate.now().toString()
            val taskUpdateValues = ContentValues().apply {
                put(ScheduleProviderContract.TASKS.DATE, today)
            }
            val updateTasksResult = applicationContext.contentResolver.update(
                ScheduleProviderContract.TASKS.CONTENT_URI, taskUpdateValues, null, null
            )
            Log.d(TAG, "Update Tasks Result: $updateTasksResult")


            val tasksCursor = applicationContext.contentResolver.query(
                ScheduleProviderContract.TASKS.CONTENT_URI, null, null, null, null
            )

            val tasks = mutableListOf<Task>()

            // TODO go through cursor
            tasksCursor?.apply {

                val taskColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_TASK)
                val priorityColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_PRIORITY)
                val statusColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_STATUS)
                val dueDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_DUE_DATE)
                val scheduledDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_SCHEDULED_DATE)
                val startDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_START_DATE)
                val evDateColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_DATE)
                val evStartTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_START_TIME)
                val evEndTimeColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_EV_END_TIME)
                val uriColumn = getColumnIndex(ScheduleProviderContract.TASKS.COLUMN_URI)


                while (moveToNext()){
                    tasks.add(Task(
                        task = getString(taskColumn),
                        priority = TaskPriority.entries.getOrNull(getInt(priorityColumn)) ?: TaskPriority.NORMAL,
                        status = getStringOrNull(statusColumn),
                        dueDate = getStringOrNull(dueDateColumn),
                        scheduledDate = getStringOrNull(scheduledDateColumn),
                        startDate = getStringOrNull(startDateColumn),
                        evDate = getStringOrNull(evDateColumn),
                        evStartTime = getStringOrNull(evStartTimeColumn),
                        evEndTime = getStringOrNull(evEndTimeColumn),
                        uri = getStringOrNull(uriColumn)?.let { Uri.parse(it) }
                    ))
                }
            }?.close() // FIXME it could be more efficient to requery the cursor instead of opening a new one every time


            val timeTasks: MutableList<Task> = mutableListOf()
            val nonTimeTasks: MutableList<Task> = mutableListOf()

            for(task in tasks){
                if(task.evDate == today && task.evStartTime != null){
                    timeTasks.add(task)
                }else{
                    nonTimeTasks.add(task)
                }
            }

            val taskDisplayManager = TaskDisplayManager(settings)

            Pair(taskDisplayManager.getTasks(timeTasks, today), taskDisplayManager.getTasks(nonTimeTasks, today))
        }
    }
    /*
        return withContext(Dispatchers.IO){
            val skipDirectories = listOf(".obsidian", ".trash")
            val settings = JSONObject(mapOf(
                "tasks_tag" to "#todo",
                "skip_directories" to JSONArray(skipDirectories).toString()
            ))


            val taskParser = TaskParser(applicationContext, settings)
            //val today = LocalDate.parse("2024-07-31")
            val today = LocalDate.now()
            val tasks: MutableList<Task>

            val getTasksTime = measureTimeMillis { tasks = taskParser.getTasks(today, uri) }
            Log.i(TAG, "Complete getTasks() function elapsed time: $getTasksTime ms")

            val timeTasks: MutableList<Task> = mutableListOf()
            val nonTimeTasks: MutableList<Task> = mutableListOf()

            for(task in tasks){
                if(task.evDate == today.toString() && task.evStartTime != null){
                    timeTasks.add(task)
                }else{
                    nonTimeTasks.add(task)
                }
            }

            val taskDisplayManager = TaskDisplayManager(settings)
            val todayString = today.toString()

            Pair(taskDisplayManager.getTasks(timeTasks, todayString), taskDisplayManager.getTasks(nonTimeTasks, todayString))
        }
    }
     */

    private fun reloadTasks(){
        MainScope().launch {
            pbLoadingWheel.visibility = View.VISIBLE
            linearLayoutMain.alpha = 0.4f

            setTodayDate()
            loadSettings()

            val settings = SettingsData(
                directories = directoryList.mapNotNull { it.uri }.toSet(),
                tasksTag = "#todo",
                skipDirectories = setOf(".obsidian", ".trash")
            )

            val (timeTasks, nonTimeTasks) = contentProviderParser.getTasks(settings, true)


            if (timeTasks.size > 0) {
                tvTimeTasksNone.visibility = View.GONE
                frameTimeContainer.visibility = View.VISIBLE
            }else{
                tvTimeTasksNone.visibility = View.VISIBLE
                frameTimeContainer.visibility = View.GONE
            }

            if (nonTimeTasks.size > 0) {
                tvNonTimeTasksNone.visibility = View.GONE
            }else{
                tvNonTimeTasksNone.visibility = View.VISIBLE
            }


            // non time tasks
            // TODO pass the time tasks to the corresponding adapter
            nonTimeTaskAdapter.reloadTasks(nonTimeTasks)


            //time tasks
            // TODO get min and max hours
            setMinMaxHours(timeTasks)
            dayViewHourAdapter.changeHours(minHour,maxHour)

            timeTaskManager.setTimeTasks(timeTasks, minHour, maxHour)


            linearLayoutMain.alpha = 1.0f
            pbLoadingWheel.visibility = View.GONE
        }
    }

    // Menu code
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.settings_option -> {
                loadSettings()
                val arr = ArrayList<DirectoryData>(directoryList)
                startActivity(Intent(this, SettingsMenu::class.java).apply {
                    putExtra("directoryList", arr)
                })
                true
            }
            R.id.reload_option -> {
                reloadTasks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}