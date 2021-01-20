package com.duke.elliot.youtubediary.database

import androidx.room.*

@Dao
interface PlaylistNextPageTokenDao {
    @Query("SELECT * FROM playlist_next_page_token WHERE channelId = :channelId LIMIT 1")
    fun getPlaylistNextPageToken(channelId: String): PlaylistNextPageToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playlistNextPageToken: PlaylistNextPageToken)

    @Update
    fun update(playlistNextPageToken: PlaylistNextPageToken)

    @Delete
    fun delete(playlistNextPageToken: PlaylistNextPageToken)
}