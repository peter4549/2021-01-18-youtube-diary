package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class YouTubeVideosViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(YouTubeVideosViewModel::class.java)) {
            return YouTubeVideosViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}