package com.duke.elliot.youtubediary.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "diary")
@Parcelize
data class Diary (
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    var updatedAt: Long,
    var content: String,
    var folderId: Long,
    var youtubeVideos: Array<DisplayVideoModel>
) : Parcelable {

    @Suppress("unused")
    fun deepCopy() = Diary(
        id = this.id,
        updatedAt = this.updatedAt,
        content = this.content,
        folderId = this.folderId,
        youtubeVideos = youtubeVideos.toList().toTypedArray()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Diary

        if (id != other.id) return false
        if (updatedAt != other.updatedAt) return false
        if (content != other.content) return false
        if (folderId != other.folderId) return false
        if (!youtubeVideos.contentEquals(other.youtubeVideos)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + youtubeVideos.contentHashCode()
        return result
    }
}