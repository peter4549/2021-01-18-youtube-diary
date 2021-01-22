package com.duke.elliot.youtubediary.diary_writing

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.duke.elliot.youtubediary.database.Diary

class DiaryWritingViewModelFactory(
    private val application: Application,
    private val originalDiary: Diary?
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(DiaryWritingViewModel::class.java)) {
            return DiaryWritingViewModel(application, originalDiary) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}