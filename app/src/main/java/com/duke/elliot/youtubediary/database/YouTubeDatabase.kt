package com.duke.elliot.youtubediary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

const val YOUTUBE_DATABASE_NAME = "youtube_database_debug:1.0.8"

@Database(entities = [DisplayPlaylistModel::class, DisplayVideoModel::class,
    UpdatedAt::class, NextPageToken::class, PlaylistNextPageToken::class],
    version = 1, exportSchema = false)
abstract class YouTubeDatabase : RoomDatabase() {
    abstract fun displayPlaylistDao(): DisplayPlaylistDao
    abstract fun displayVideoDao(): DisplayVideoDao
    abstract fun updatedAtDao(): UpdatedAtDao
    abstract fun nextPageTokenDao(): NextPageTokenDao
    abstract fun playlistNextPageTokenDao(): PlaylistNextPageTokenDao

    companion object {
        @Volatile
        private var INSTANCE: YouTubeDatabase? = null

        fun getInstance(context: Context): YouTubeDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        YouTubeDatabase::class.java,
                        YOUTUBE_DATABASE_NAME
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}