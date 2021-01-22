package com.duke.elliot.youtubediary.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "diary")
@Parcelize
data class Diary (
    @PrimaryKey val id: Long = 0L,
    var updatedAt: Long,
    var content: String,
    var folder: String,
    var youtubeVideos: List<DisplayVideoModel>
) : Parcelable