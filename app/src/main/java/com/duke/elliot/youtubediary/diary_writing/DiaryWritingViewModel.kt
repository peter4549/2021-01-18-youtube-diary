package com.duke.elliot.youtubediary.diary_writing

import android.app.Application
import androidx.lifecycle.ViewModel
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel

class DiaryWritingViewModel(application: Application, originalDiary: Diary?): ViewModel() {

    val folderDao = AppDatabase.getInstance(application).folderDao()

    var updatedAt = System.currentTimeMillis()
    var content = blank
    var folder = blank
    val youtubeVideos = mutableListOf<DisplayVideoModel>()

    init {
        originalDiary?.let {
            updatedAt = it.updatedAt
            content = it.content
            folder = it.folder
            youtubeVideos.addAll(it.youtubeVideos)
        }
    }

    fun addVideo(video: DisplayVideoModel) {
        youtubeVideos.add(0, video)
    }

    companion object {
        private const val blank = ""
    }
}