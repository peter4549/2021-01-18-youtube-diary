package com.duke.elliot.youtubediary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.duke.elliot.youtubediary.database.youtube.*

const val APP_DATABASE_NAME = "com.duke.elliot.youtubediary.database.app_database_debug:1.1.1"

@Database(entities = [DisplayPlaylistModel::class, DisplayVideoModel::class,
    UpdatedAt::class, NextPageToken::class,
    Folder::class],
    version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun displayPlaylistDao(): DisplayPlaylistDao
    abstract fun displayVideoDao(): DisplayVideoDao
    abstract fun updatedAtDao(): UpdatedAtDao
    abstract fun nextPageTokenDao(): NextPageTokenDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        APP_DATABASE_NAME
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