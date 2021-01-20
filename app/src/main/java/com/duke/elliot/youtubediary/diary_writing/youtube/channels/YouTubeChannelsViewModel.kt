package com.duke.elliot.youtubediary.diary_writing.youtube.channels

import androidx.lifecycle.ViewModel
import com.duke.elliot.youtubediary.database.DisplayChannelModel
import com.duke.elliot.youtubediary.diary_writing.youtube.firestore.FireStoreHelper
import com.duke.elliot.youtubediary.main.MainApplication

class YouTubeChannelsViewModel: ViewModel() {
    val channels = mutableListOf<DisplayChannelModel>()
    val fireStoreHelper = FireStoreHelper()
    val firebaseAuth = MainApplication.getFirebaseAuthInstance()

    fun registerFireStoreHelper(uid: String, onDocumentSnapshotListener: FireStoreHelper.OnDocumentSnapshotListener) {
        fireStoreHelper.setUserSnapshotListener(uid)
        fireStoreHelper.setOnDocumentSnapshotListener(onDocumentSnapshotListener)
    }
}