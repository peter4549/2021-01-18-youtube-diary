package com.duke.elliot.youtubediary.database.youtube

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface DisplayVideoDao {
    @Query("SELECT * FROM display_video_model WHERE channelId = :channelId")
    fun getAllByChannelId(channelId: String): MutableList<DisplayVideoModel>

    @Query("SELECT * FROM display_video_model WHERE playlistId = :playlistId")
    fun getAllByPlaylistId(playlistId: String): MutableList<DisplayVideoModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(displayVideoModels: List<DisplayVideoModel>)

    @Query("DELETE FROM display_video_model WHERE id IN (:ids)")
    fun deleteAll(ids: List<String>)

    @Query("SELECT COUNT(id) FROM display_video_model WHERE playlistId = :playlistId")
    fun getItemCountByPlaylistId(playlistId: String): Int

    @Query("SELECT COUNT(id) FROM display_video_model WHERE channelId = :channelId")
    fun getItemCountByChannelId(channelId: String): Int
}