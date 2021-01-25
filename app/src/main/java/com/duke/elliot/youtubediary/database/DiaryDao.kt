package com.duke.elliot.youtubediary.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY updatedAt DESC")
    fun getAll(): LiveData<List<Diary>>

    @Insert
    fun insert(diary: Diary)

    @Delete
    fun delete(diary: Diary)

    @Update
    fun update(diary: Diary)

    @Query("UPDATE diary SET folderId = :to WHERE folderId = :from")
    fun changeFolderId(from: Long, to: Long)

    @Query("SELECT COUNT(id) FROM diary")
    fun getDiaryCount(): Long
}