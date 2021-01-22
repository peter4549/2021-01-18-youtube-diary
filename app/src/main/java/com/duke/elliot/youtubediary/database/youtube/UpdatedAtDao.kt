package com.duke.elliot.youtubediary.database.youtube

import androidx.room.*

@Dao
interface UpdatedAtDao {
    @Query("SELECT * FROM updated_at WHERE id = :id AND kind = :kind LIMIT 1")
    fun getUpdatedAt(id: String, kind: String): UpdatedAt?

    @Query("SELECT * FROM updated_at")
    fun getAll(): List<UpdatedAt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(updatedAt: UpdatedAt)

    @Update
    fun update(updatedAt: UpdatedAt)

    @Delete
    fun delete(updatedAt: UpdatedAt)
}