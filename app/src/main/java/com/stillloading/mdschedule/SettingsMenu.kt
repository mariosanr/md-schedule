package com.stillloading.mdschedule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.data.SettingsDisplayData
import com.stillloading.mdschedule.data.SkipDirectoryData
import com.stillloading.mdschedule.data.UpdateTimesData
import com.stillloading.mdschedule.settingsutils.DirectoryAdapter
import com.stillloading.mdschedule.settingsutils.SkipDirectoriesAdapter
import com.stillloading.mdschedule.settingsutils.UpdateTimesAdapter
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import java.util.Calendar


class SettingsMenu : AppCompatActivity() {

    private val TAG = "Md Companion Debug"

    private lateinit var contentProviderParser: ContentProviderParser

    private lateinit var directoryAdapter: DirectoryAdapter
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private lateinit var updateTimesAdapter: UpdateTimesAdapter
    private lateinit var skipDirectoriesAdapter: SkipDirectoriesAdapter

    private lateinit var etTasksTag: EditText

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchDayPlannerNotifications: SwitchCompat
    private lateinit var switchInProgressTasks: SwitchCompat
    private lateinit var switchDayPlannerWidget: SwitchCompat

    private lateinit var rlDayPlannerNotifications: RelativeLayout


    private lateinit var settings: SettingsDisplayData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)


        contentProviderParser = ContentProviderParser(applicationContext)
        settings = contentProviderParser.getSettings()

        initFolderPaths()
        initTasksTag()
        initDayPlannerNotifications()
        initNotifications()
        initUpdateTimes()
        initInProgressTasks()
        initDayPlannerWidget()
        initSkipDirectories()
    }


    override fun onPause() {
        super.onPause()

        settings.directories = directoryAdapter.directoryList
        settings.tasksTag = etTasksTag.text.toString().trim()
        settings.notificationsEnabled = switchNotifications.isChecked
        settings.dayPlannerNotificationsEnabled = switchDayPlannerNotifications.isChecked
        settings.updateTimes = updateTimesAdapter.updateTimesList
        settings.inProgressTasksEnabled = switchInProgressTasks.isChecked
        settings.dayPlannerWidgetEnabled = switchDayPlannerWidget.isChecked
        settings.skipDirectories = skipDirectoriesAdapter.skipDirectoryList.map {
            SkipDirectoryData(text = it.editableText.toString().trim())
        }.toMutableList()

        contentProviderParser.saveSettings(settings)
    }



    private fun initFolderPaths(){
        directoryAdapter = DirectoryAdapter(settings.directories)

        val rvSettingsPath = findViewById<RecyclerView>(R.id.rvSettingsPaths)
        rvSettingsPath.adapter = directoryAdapter
        rvSettingsPath.layoutManager = LinearLayoutManager(this)

        val bAddPath = findViewById<Button>(R.id.bAddPath)
        bAddPath.setOnClickListener {
            openDirectory()
        }

        // register to be able to pop up system file picker
        startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data.also { uri ->
                    addPath(uri?.data)
                }
            }
        }
    }

    private fun addPath(uri: Uri?){
        if(uri == null){
            return
        }

        val contentResolver = applicationContext.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        // Check for the freshest data.
        contentResolver.takePersistableUriPermission(uri, takeFlags)


        Log.d(TAG, "addPath: $uri")

        val path: String = uri.path.toString().substringAfter(":")
        directoryAdapter.addItem(DirectoryData(uri, path))
    }

    private fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        startForResult.launch(intent)
    }



    private fun initTasksTag(){
        etTasksTag = findViewById(R.id.etTasksTag)
        etTasksTag.setText(settings.tasksTag)
    }

    private fun initDayPlannerNotifications(){
        rlDayPlannerNotifications = findViewById(R.id.rlDayPlannerNotifications)
        rlDayPlannerNotifications.visibility = View.GONE

        switchDayPlannerNotifications = findViewById(R.id.switchDayPlannerNotifications)
        switchDayPlannerNotifications.isChecked = settings.dayPlannerNotificationsEnabled
    }

    private fun initNotifications(){
        switchNotifications = findViewById(R.id.switchNotifications)

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                rlDayPlannerNotifications.visibility = View.VISIBLE
                // call permission
            }else{
                rlDayPlannerNotifications.visibility = View.GONE
            }
        }

        switchNotifications.isChecked = settings.notificationsEnabled
    }

    private fun initUpdateTimes(){
        updateTimesAdapter = UpdateTimesAdapter(settings.updateTimes)

        val rvSettingsUpdateTimes = findViewById<RecyclerView>(R.id.rvSettingsUpdateTimes)
        rvSettingsUpdateTimes.adapter = updateTimesAdapter
        rvSettingsUpdateTimes.layoutManager = LinearLayoutManager(this)

        val bAddUpdateTime = findViewById<Button>(R.id.bAddUpdateTimes)
        bAddUpdateTime.setOnClickListener {
            setUpdateTime(updateTimesAdapter)
        }

    }

    @SuppressLint("DefaultLocale")
    private fun setUpdateTime(updateTimesAdapter: UpdateTimesAdapter){
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            updateTimesAdapter.addItem(UpdateTimesData(timeString = formattedTime))
        }, hour, minutes, true)

        timePickerDialog.show()
    }



    private fun initInProgressTasks(){
        switchInProgressTasks = findViewById(R.id.switchInProgressTasks)
        switchInProgressTasks.isChecked = settings.inProgressTasksEnabled
    }

    private fun initDayPlannerWidget(){
        switchDayPlannerWidget = findViewById(R.id.switchDayPlannerWidget)
        switchDayPlannerWidget.isChecked = settings.dayPlannerWidgetEnabled
    }

    private fun initSkipDirectories(){
        skipDirectoriesAdapter = SkipDirectoriesAdapter(settings.skipDirectories)

        val rvSettingsSkipDirectories = findViewById<RecyclerView>(R.id.rvSettingsSkipDirectories)
        rvSettingsSkipDirectories.adapter = skipDirectoriesAdapter
        rvSettingsSkipDirectories.layoutManager = LinearLayoutManager(this)

        val bAddSkipDirectory = findViewById<Button>(R.id.bAddSkipDirectory)
        bAddSkipDirectory.setOnClickListener {
            skipDirectoriesAdapter.addItem(SkipDirectoryData())
        }

    }



    // Back Button code
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //startActivity(Intent(this, MainActivity::class.java))
        finish()
        return super.onOptionsItemSelected(item)
    }

}