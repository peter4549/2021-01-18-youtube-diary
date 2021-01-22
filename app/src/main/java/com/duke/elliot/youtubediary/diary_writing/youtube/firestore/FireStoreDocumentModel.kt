package com.duke.elliot.youtubediary.diary_writing.youtube.firestore

import com.duke.elliot.youtubediary.database.youtube.DisplayChannelModel

data class UserModel(var uid: String,
                     var premium: Boolean,
                     var youtubeChannels: MutableList<DisplayChannelModel> = mutableListOf()
){
    companion object {
        const val FIELD_UID = "uid"
        const val FILED_PREMIUM = "premium"
        const val FILED_YOUTUBE_CHANNELS = "youtubeChannels"
    }
}