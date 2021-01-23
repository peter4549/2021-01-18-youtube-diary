package com.duke.elliot.youtubediary.diary_writing

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiaryWritingViewModel(application: Application, val originalDiary: Diary?): ViewModel() {

    val folderDao = AppDatabase.getInstance(application).folderDao()

    var updatedAt = System.currentTimeMillis()
    var content = blank
    var folder: Folder? = null
    val youtubeVideos = mutableListOf<DisplayVideoModel>()

    init {
        originalDiary?.let {
            updatedAt = it.updatedAt
            content = it.content
            folder = it.folder
            youtubeVideos.addAll(it.youtubeVideos)  // Deep copy.
        }
    }

    fun addVideo(video: DisplayVideoModel) {
        youtubeVideos.add(video)
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                folderDao.delete(folder)
            }
        }
    }

    companion object {
        private const val blank = ""
    }
}