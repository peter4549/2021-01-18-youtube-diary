package com.duke.elliot.youtubediary.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FolderDao {

    @Query("SELECT * from folder ORDER BY name ASC")
    fun getAll(): LiveData<List<Folder>>

    @Insert
    fun insert(folder: Folder)

    @Delete
    fun delete(folder: Folder)

    @Update
    fun update(folder: Folder)
}