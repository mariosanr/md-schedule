package com.stillloading.mdschedule

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stillloading.mdschedule.data.DirectoryData
import com.stillloading.mdschedule.data.SettingsData
import com.stillloading.mdschedule.data.toContentValues
import com.stillloading.mdschedule.systemutils.ContentProviderParser
import com.stillloading.mdschedule.systemutils.ScheduleProviderContract
import org.json.JSONArray
import java.io.File


class SettingsMenu : AppCompatActivity() {

    private val TAG = "Md Companion Debug"

    private lateinit var contentProviderParser: ContentProviderParser

    private lateinit var directoryAdapter: DirectoryAdapter
    private val settingFilename = "paths_settings"
    private lateinit var startForResult: ActivityResultLauncher<Intent>


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

        // get the list of directories from the main activity
        val directoryArrayList = IntentCompat.getParcelableArrayListExtra(intent, "directoryList", DirectoryData::class.java)
        val directoryList = directoryArrayList?.toMutableList() ?: mutableListOf()

        directoryAdapter = DirectoryAdapter(directoryList)

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


    override fun onPause() {
        super.onPause()

        contentProviderParser.saveSettings(directoryAdapter.directoryList)
    }

    /*
    private fun saveSettings(){
        val fileContentList: MutableList<Map<String, String>> = mutableListOf()
        for(directoryData in directoryAdapter.directoryList){
            fileContentList.add(
                mapOf("uri" to directoryData.uri.toString(), "path" to directoryData.text, "position" to directoryData.position.toString())
            )
        }

        val fileContents = JSONArray(fileContentList).toString()
        //val fileContentsJSONObject = JSONObject(mapOf("pathsList" to JSONArray(fileContentList)))
        //val fileContents = fileContentsJSONObject.toString()

        // this should work, but I prefer that the other one explicitly overwrites the file
        /*
        applicationContext.openFileOutput(settingFilename, MODE_PRIVATE).use {
            it.write(fileContents.toByteArray(Charsets.UTF_8))
        }
         */

        val file = File(applicationContext.filesDir, settingFilename)
        file.createNewFile()
        file.writeText(fileContents, Charsets.UTF_8)
    }

     */


    private fun addPath(uri: Uri?){
        // TODO get in the file system and ask them to set a directory to look in
        if(uri == null){
            return
        }

        val contentResolver = applicationContext.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        // Check for the freshest data.
        contentResolver.takePersistableUriPermission(uri, takeFlags)


        Log.d(TAG, "addPath: $uri")

        // TODO investigar cual de estos o algo parecido puedo usar para remplazar el hardcoded string que tengo allá abajo
        //Environment.getExternalStorageDirectory()
        //Environment.DIRECTORY_DOWNLOADS
        val path: String = uri.path.toString().replace("/tree/primary:", "")
        directoryAdapter.addItem(DirectoryData(uri, path))
    }

    private fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        startForResult.launch(intent)
    }

    // Back Button code
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //startActivity(Intent(this, MainActivity::class.java))
        finish()
        return super.onOptionsItemSelected(item)
    }

}