package com.duke.elliot.youtubediary.database

import androidx.room.*

@Dao
interface NextPageTokenDao {
    @Query("SELECT * FROM next_page_token WHERE id = :id AND kind = :kind LIMIT 1")
    fun getNextPageToken(id: String, kind: String): NextPageToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(nextPageToken: NextPageToken)

    @Query("SELECT * FROM next_page_token")
    fun getAll(): List<NextPageToken>

    @Update
    fun update(nextPageToken: NextPageToken)

    @Delete
    fun delete(nextPageToken: NextPageToken)
}