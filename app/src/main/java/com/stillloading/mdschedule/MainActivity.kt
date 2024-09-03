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

package com.stillloading.mdschedule

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.TaskDisplayData
import com.stillloading.mdschedule.databinding.PopupTaskBinding
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import com.stillloading.mdschedule.systemutils.FileSystemManager
import com.stillloading.mdschedule.taskutils.DayViewHourAdapter
import com.stillloading.mdschedule.taskutils.NonTimeTaskAdapter
import com.stillloading.mdschedule.taskutils.TaskPopup
import com.stillloading.mdschedule.taskutils.TimeTaskManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val TAG = "Md Companion Debug"

    private lateinit var nonTimeTaskAdapter: NonTimeTaskAdapter
    private lateinit var dayViewHourAdapter: DayViewHourAdapter
    private lateinit var timeTaskManager: TimeTaskManager
    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var contentProviderParser: ContentProviderParser

    private lateinit var tvDate: TextView
    private lateinit var tvLastUpdated: TextView

    private lateinit var linearLayoutMain: LinearLayout
    private lateinit var rvTaskList: RecyclerView
    private lateinit var rvHourList: RecyclerView
    private lateinit var frameTimeContainer: FrameLayout
    private lateinit var pbLoadingWheel: ProgressBar

    private lateinit var tvTimeTasksNone: TextView
    private lateinit var tvNonTimeTasksNone: TextView

    private var minHour: Int = -1
    private var maxHour: Int = -1

    private var updatingTasks = false

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
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        linearLayoutMain = findViewById(R.id.linearLayoutMain)
        tvTimeTasksNone = findViewById(R.id.tvTimeTasksNone)
        tvNonTimeTasksNone = findViewById(R.id.tvNonTimeTasksNone)

        linearLayoutMain.visibility = View.GONE


        setTodayDate()

        fileSystemManager = FileSystemManager(applicationContext)
        contentProviderParser = ContentProviderParser(applicationContext)


        val popupBinding = PopupTaskBinding.inflate(layoutInflater)
        val taskPopup = TaskPopup(popupBinding, this)

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


        //reloadTasks(update = false, firstLaunch = true)
    }

    override fun onStart() {
        super.onStart()

        if(!updatingTasks){
            reloadTasks(false)
        }


        if(timeTaskManager.timeBarView != null){
            timeTaskManager.setTimeBar(minHour, maxHour)
        }
    }


    private fun setTodayDate(date: LocalDate = LocalDate.now()){
        val pattern = DateTimeFormatter.ofPattern("'Today:' EEE, MMM d, uuuu")
        val formattedDate = date.format(pattern)
        tvDate.text = formattedDate
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


    private fun reloadTasks(update: Boolean, firstLaunch: Boolean = false){
        MainScope().launch {
            pbLoadingWheel.visibility = View.VISIBLE
            linearLayoutMain.alpha = 0.4f
            updatingTasks = true

            // if not updating, use the last modified date, since that is what the tasks in the database were made with
            val date = if(update)
                LocalDate.now() else contentProviderParser.getLastUpdated()?.toLocalDate() ?: LocalDate.now()

            val (timeTasks, nonTimeTasks) = contentProviderParser.getTasks(date.toString(), update) ?: run {
                pbLoadingWheel.visibility = View.GONE
                linearLayoutMain.alpha = 1.0f
                updatingTasks = false

                return@launch
            }




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
            nonTimeTaskAdapter.reloadTasks(nonTimeTasks)


            //time tasks
            setMinMaxHours(timeTasks)
            dayViewHourAdapter.changeHours(minHour,maxHour)

            timeTaskManager.setTimeTasks(timeTasks, minHour, maxHour)


            setTodayDate()

            val lastUpdated = contentProviderParser.getLastUpdatedString()
            if(lastUpdated != null){
                tvLastUpdated.text = lastUpdated
                tvLastUpdated.visibility = View.VISIBLE
            }else{
                tvLastUpdated.visibility = View.GONE
            }

            linearLayoutMain.visibility = View.VISIBLE
            linearLayoutMain.alpha = 1.0f
            pbLoadingWheel.visibility = View.GONE


            if(firstLaunch){
                reloadTasks(true, firstLaunch = false)
            }

            updatingTasks = false
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
                startActivity(Intent(this, SettingsMenu::class.java))
                true
            }
            R.id.reload_option -> {
                reloadTasks(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}