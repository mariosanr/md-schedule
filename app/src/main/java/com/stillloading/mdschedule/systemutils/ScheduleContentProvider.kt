package com.stillloading.mdschedule.systemutils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import com.stillloading.mdschedule.data.SettingsFlowData
import com.stillloading.mdschedule.data.toSettingsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

object ScheduleProviderContract{
    const val AUTHORITY = "com.stillloading.mdschedule.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    const val PATH_SETTINGS = "settings"
    const val PATH_TASKS = "tasks"
    const val PATH_LAST_UPDATED = "last_updated"


    const val CODE_ERROR = 0
    const val CODE_SUCCESS = 1
    const val CODE_UPDATING = 2

    object SETTINGS{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SETTINGS).build()

        const val DIRECTORIES = "directories"
        const val TASKS_TAG = "tasks_tag"
        const val SKIP_DIRECTORIES = "skip_directories"
    }

    object TASKS: BaseColumns{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TASKS).build()

        const val DATE = "date"

        const val _ID = BaseColumns._ID
        const val COLUMN_TASK = "task"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_STATUS = "status"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_SCHEDULED_DATE = "scheduled_date"
        const val COLUMN_START_DATE = "start_date"
        const val COLUMN_EV_DATE = "ev_date"
        const val COLUMN_EV_START_TIME = "ev_start_time"
        const val COLUMN_EV_END_TIME = "ev_end_time"
        const val COLUMN_URI = "uri"
    }

    object LAST_UPDATED{
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LAST_UPDATED).build()

        const val COLUMN_DATETIME = "date_time"
    }

}

private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_SETTINGS, 1)
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_TASKS, 2)
    addURI(ScheduleProviderContract.AUTHORITY, ScheduleProviderContract.PATH_LAST_UPDATED, 3)
}

class ScheduleContentProvider : ContentProvider() {

    private val TAG = "Schedule Provider"

    private lateinit var fileSystemManager: FileSystemManager

    //Room database
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var taskDao: TaskDao
    private var updatingDB = false

    // Preferences Data Store
    private lateinit var settingsFlowData: SettingsFlowData
    private lateinit var lastUpdatedFlow: Flow<String>


    override fun onCreate(): Boolean {
        fileSystemManager = FileSystemManager(context!!)

        taskDatabase = TaskDatabase.getDatabase(context!!)
        /*
        taskDatabase = Room.databaseBuilder(
            context!!,
            TaskDatabase::class.java,
            DBNAME
        ).build()
         */
        taskDao = taskDatabase.taskDao()

        settingsFlowData = fileSystemManager.getSettingsFlow()
        lastUpdatedFlow = fileSystemManager.getLastUpdatedFlow()

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        context?.enforceCallingOrSelfPermission("com.stillloading.mdschedule.provider.READ_PROVIDER", "Read permission denied")
        return when(uriMatcher.match(uri)){
            1 -> { // settings
                runBlocking(Dispatchers.IO) {
                    val cursor = MatrixCursor(arrayOf("key", "value"))

                    val settingsMap = fileSystemManager.getSettingsMap(settingsFlowData)
                    settingsMap.forEach { (key, value) ->
                        cursor.addRow(arrayOf(key, value))
                    }

                    // Returns the as strings. The settings that are lists are a comma separated string
                    cursor
                }
            }
            2 -> { // tasks
                taskDao.getAll()
            }
            3 -> {
                runBlocking(Dispatchers.IO) {
                    val cursor = MatrixCursor(arrayOf(ScheduleProviderContract.LAST_UPDATED.COLUMN_DATETIME)).apply {
                        addRow(arrayOf(fileSystemManager.getLastUpdated(lastUpdatedFlow)))
                    }

                    cursor
                }
            }
            else -> { // not recognized
                throw IllegalArgumentException()
            }
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        context?.enforceCallingOrSelfPermission("com.stillloading.mdschedule.provider.WRITE_PROVIDER", "Write permission denied")
        return when(uriMatcher.match(uri)){
            1 -> { // settings
                // TODO use the values parameter
                runBlocking(Dispatchers.IO){
                    values?.toSettingsData()?.let { fileSystemManager.saveSettings(it) }
                }

                context?.contentResolver?.notifyChange(uri, null)
                ScheduleProviderContract.CODE_SUCCESS
            }
            2 -> { // tasks
                if(!updatingDB){
                    updatingDB = true
                    taskDao.deleteAll()

                    runBlocking(Dispatchers.IO) {
                        val date = values?.getAsString(ScheduleProviderContract.TASKS.DATE)
                        if(date != null){
                            val settings = fileSystemManager.getSettingsData(settingsFlowData)

                            taskDao.insertAll(*fileSystemManager.getTasksArray(settings, date))
                            fileSystemManager.saveLastUpdated(LocalDateTime.now())
                        }
                    }
                    updatingDB = false
                    context?.contentResolver?.notifyChange(uri, null)
                    return ScheduleProviderContract.CODE_SUCCESS
                }else{
                    return ScheduleProviderContract.CODE_UPDATING
                }
            }
            else -> { // not recognized
                ScheduleProviderContract.CODE_ERROR
            }
        }
    }
}