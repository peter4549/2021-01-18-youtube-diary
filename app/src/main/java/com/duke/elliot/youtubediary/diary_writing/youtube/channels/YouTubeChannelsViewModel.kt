package com.duke.elliot.youtubediary.diary_writing.youtube.channels

import androidx.lifecycle.ViewModel
import com.duke.elliot.youtubediary.diary_writing.youtube.ChannelModel

class YouTubeChannelsViewModel: ViewModel() {
    val channels = mutableListOf<ChannelModel>()
}