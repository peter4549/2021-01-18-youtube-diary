package com.duke.elliot.youtubediary.diary_writing.youtube.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class YouTubeChannelsViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(YouTubeChannelsViewModel::class.java)) {
            return YouTubeChannelsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}