package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import androidx.lifecycle.ViewModel

class YouTubeVideosViewModel: ViewModel() {
    var channelId: String? = null
    var playlistId: String? = null
    var displayPlaylists: ArrayList<DisplayPlaylistModel> = arrayListOf()

    val playlistIdNextPageTokenMap = mutableMapOf<String?, String?>()
}