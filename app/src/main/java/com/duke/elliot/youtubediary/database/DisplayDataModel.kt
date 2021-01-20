package com.duke.elliot.youtubediary.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

data class DisplayChannelModel (
        val id: String,
        val title: String,
        val thumbnailUri: String
)

@Entity(tableName = "display_playlist_model")
data class DisplayPlaylistModel (
        @PrimaryKey val id: String,
        val channelId: String,
        val title: String,
        val description: String,
        val thumbnailUri: String,
)

@Parcelize
@Entity(tableName = "display_video_model")
data class DisplayVideoModel(
        @PrimaryKey val id: String,
        val title: String,
        val thumbnailUri: String?,
        val timeAgo: String,
        val channelId: String?,
        val playlistId: String?,
        val kind: String,
): Parcelable

@Entity(tableName = "next_page_token")
data class NextPageToken(
        @PrimaryKey val id: String,
        val nextPageToken: String,
        val kind: String
)

@Entity(tableName = "updated_at")
data class UpdatedAt(
        @PrimaryKey val id: String,
        val updatedAt: Long,
        val kind: String
)

@Entity(tableName = "playlist_next_page_token")
data class PlaylistNextPageToken(
        @PrimaryKey val channelId: String,
        val nextPageToken: String
)