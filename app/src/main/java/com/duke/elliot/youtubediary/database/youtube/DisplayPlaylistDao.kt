package com.duke.elliot.youtubediary.database.youtube

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DisplayPlaylistDao {
    @Query("SELECT * FROM display_playlist_model WHERE channelId = :channelId")
    fun getAllByChannelId(channelId: String): MutableList<DisplayPlaylistModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(displayPlaylistModels: List<DisplayPlaylistModel>)

    @Query("delete from display_playlist_model where id in (:ids)")
    fun deleteAll(ids: List<String>)

    @Query("SELECT COUNT(id) FROM display_playlist_model")
    fun getItemCount(): Int
}