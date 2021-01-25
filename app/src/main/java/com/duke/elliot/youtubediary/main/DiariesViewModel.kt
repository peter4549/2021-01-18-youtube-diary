package com.duke.elliot.youtubediary.main

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.DEFAULT_FOLDER_ID
import com.duke.elliot.youtubediary.database.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class DiariesViewModel(application: Application): ViewModel() {
    private val appDatabase = AppDatabase.getInstance(application)
    private val folderDao = appDatabase.folderDao()
    private val diaryDao = appDatabase.diaryDao()
    val diaries = diaryDao.getAll()
    val folders = folderDao.getAll()
    var currentFolder: Folder? = null

    suspend fun deleteFolder(folder: Folder): Boolean = withContext(Dispatchers.IO) {
        try {
            folderDao.delete(folder)
            diaryDao.changeFolderId(folder.id, DEFAULT_FOLDER_ID)
            true
        } catch (e: Exception) {
            false
        }
    }
}