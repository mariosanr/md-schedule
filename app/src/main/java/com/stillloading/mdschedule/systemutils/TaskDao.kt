package com.stillloading.mdschedule.systemutils

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAll(): Cursor

    @Insert
    fun insertAll(vararg task: TaskEntityData)

    @Query("DELETE FROM tasks")
    fun deleteAll()
}