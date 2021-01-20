package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class YouTubeVideosViewModelFactory(private val application: Application, private val channelId: String): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(YouTubeVideosViewModel::class.java)) {
            return YouTubeVideosViewModel(application, channelId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}