package com.duke.elliot.youtubediary.main.diaries

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DiariesViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(DiariesViewModel::class.java)) {
            return DiariesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}