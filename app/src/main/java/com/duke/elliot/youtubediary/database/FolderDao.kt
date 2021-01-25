package com.duke.elliot.youtubediary.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FolderDao {

    @Query("SELECT * from folder ORDER BY name ASC")
    fun getAll(): LiveData<List<Folder>>

    @Query("SELECT * from folder ORDER BY name ASC")
    fun getAllValue(): List<Folder>

    @Insert
    fun insert(folder: Folder)

    @Query("SELECT * FROM folder WHERE id = :id LIMIT 1")
    fun getFolder(id: Long): LiveData<Folder?>

    @Query("SELECT * FROM folder WHERE id = :id LIMIT 1")
    fun getFolderValue(id: Long): Folder?

    @Delete
    fun delete(folder: Folder)

    @Update
    fun update(folder: Folder)
}