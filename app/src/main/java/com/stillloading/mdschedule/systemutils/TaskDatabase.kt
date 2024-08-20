package com.stillloading.mdschedule.systemutils

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntityData::class], version = 1)
abstract class TaskDatabase : RoomDatabase(){
    abstract fun taskDao(): TaskDao

    companion object {
        private val dbName = "tasks.db"
        private var instance: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase{
            if(instance == null){
                instance = Room.databaseBuilder(
                    context,
                    TaskDatabase::class.java,
                    dbName
                ).build()
            }
            return instance as TaskDatabase
        }
    }
}