package com.duke.elliot.youtubediary.database.youtube

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.COLLECTION_CHANNEL
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.COLLECTION_PLAYLIST


@Dao
interface DisplayVideoDao {
    @Query("SELECT * FROM display_video_model WHERE channelId = :channelId")
    fun getAllByChannelId(channelId: String): MutableList<DisplayVideoModel>

    @Query("SELECT * FROM display_video_model WHERE playlistId = :playlistId")
    fun getAllByPlaylistId(playlistId: String): MutableList<DisplayVideoModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(displayVideoModels: List<DisplayVideoModel>)

    @Query("DELETE FROM display_video_model WHERE channelId = :channelId AND collection = :collection")
    fun deleteAllByChannelId(channelId: String, collection: String = COLLECTION_CHANNEL)

    @Query("DELETE FROM display_video_model WHERE playlistId = :playlistId AND collection = :collection")
    fun deleteAllByPlaylistId(playlistId: String, collection: String = COLLECTION_PLAYLIST)

    @Query("SELECT COUNT(id) FROM display_video_model WHERE playlistId = :playlistId")
    fun getItemCountByPlaylistId(playlistId: String): Int

    @Query("SELECT COUNT(id) FROM display_video_model WHERE channelId = :channelId")
    fun getItemCountByChannelId(channelId: String): Int
}