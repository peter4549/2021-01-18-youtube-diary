package com.duke.elliot.youtubediary.diary_writing

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.DEFAULT_FOLDER_ID
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiaryWritingViewModel(application: Application, val originalDiary: Diary?): ViewModel() {

    private val appDatabase = AppDatabase.getInstance(application)
    private val diaryDao = appDatabase.diaryDao()
    private val folderDao = appDatabase.folderDao()
    val folder = MutableLiveData<Folder?>()

    var updatedAt = System.currentTimeMillis()
    var originalContent = blank
    val originalFolderId: Long by lazy {
        originalDiary?.folderId ?: DEFAULT_FOLDER_ID
    }
    val youtubeVideos = mutableListOf<DisplayVideoModel>()

    init {
        originalDiary?.let {
            updatedAt = it.updatedAt
            originalContent = it.content
            youtubeVideos.addAll(it.youtubeVideos)  // Deep copy.
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val value = folderDao.getFolderValue(originalFolderId)
                withContext(Dispatchers.Main) {
                    folder.value = value
                }
            }
        }
    }

    fun addVideo(position: Int = 0, video: DisplayVideoModel) {
        youtubeVideos.add(position, video)
    }

    fun deleteFolder(folderDeleted: Folder) {
        viewModelScope.launch {
            if (folderDeleted == folder.value)
                folder.value = null

            withContext(Dispatchers.IO) {
                folderDao.delete(folderDeleted)
                diaryDao.changeFolderId(folderDeleted.id, DEFAULT_FOLDER_ID)
            }
        }
    }

    fun saveDiary(diary: Diary) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                diaryDao.insert(diary)

                if (diary.folderId != DEFAULT_FOLDER_ID) {
                    folder.value?.let {
                        val diaryIds = it.diaryIds.toMutableList()

                        if (!diaryIds.contains(diary.id)) {
                            diaryIds.add(diary.id)
                            it.diaryIds = diaryIds.toTypedArray()
                            folderDao.update(it)
                        }
                    }
                }
            }
        }
    }

    fun updateDiary(diary: Diary) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                diaryDao.update(diary)

                if (diary.folderId != DEFAULT_FOLDER_ID) {
                    folder.value?.let {
                        val diaryIds = it.diaryIds.toMutableList()

                        if (!diaryIds.contains(diary.id)) {
                            diaryIds.add(diary.id)
                            it.diaryIds = diaryIds.toTypedArray()
                            folderDao.update(it)
                        }
                    }

                    originalFolderId.let {
                        val originalFolder = folderDao.getFolderValue(it)
                        val diaryIds = originalFolder?.diaryIds?.toMutableList() ?: return@let

                        if (diaryIds.contains(diary.id)) {
                            diaryIds.remove(diary.id)
                            originalFolder.diaryIds = diaryIds.toTypedArray()
                            folderDao.update(originalFolder)
                        }
                    }
                }
            }
        }
    }

    fun noFolder() = folder.value == null
    fun noYouTubeVideos() = youtubeVideos.isNullOrEmpty()

    companion object {
        private const val blank = ""
    }
}