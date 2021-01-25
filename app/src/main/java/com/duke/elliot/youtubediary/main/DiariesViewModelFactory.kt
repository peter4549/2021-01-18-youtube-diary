package com.duke.elliot.youtubediary.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.diary_writing.DiaryWritingViewModel

class DiariesViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(DiariesViewModel::class.java)) {
            return DiariesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}