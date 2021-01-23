package com.duke.elliot.youtubediary.database

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun longArrayToJson(value: Array<Long>): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToLongArray(value: String): Array<Long> = Gson().fromJson(value, Array<Long>::class.java)

    @TypeConverter
    fun folderToJson(value: Folder): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToFolder(value: String): Folder {
        return Gson().fromJson(value, Folder::class.java)
    }
}