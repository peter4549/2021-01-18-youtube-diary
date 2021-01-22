package com.duke.elliot.youtubediary.database

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "folder")
@Parcelize
data class Folder(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        var name: String,
        @ColorInt var color: Int,
        var diaryIds: Array<Long> = arrayOf()
): Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Folder

        if (name != other.name) return false
        if (color != other.color) return false
        if (!diaryIds.contentEquals(other.diaryIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + color
        result = 31 * result + diaryIds.contentHashCode()
        return result
    }
}