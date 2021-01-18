package com.duke.elliot.youtubediary.diary_writing.youtube.player

import android.os.Bundle
import android.widget.Toast
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.YouTubeVideosActivity
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.android.synthetic.main.activity_youtube_player.*
import timber.log.Timber

class YouTubePlayerActivity : YouTubeBaseActivity() {

    private var videoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_player)

        videoId = intent.getStringExtra(YouTubeVideosActivity.EXTRA_NAME_VIDEO_ID)

        if (videoId.isNullOrBlank()) {
            showToast(getString(R.string.video_cannot_be_played))
            finish()
        }

        youtube_player_view.initialize(TAG, object : YouTubePlayer.OnInitializedListener {
            override fun onInitializationSuccess(
                provider: YouTubePlayer.Provider?,
                player: YouTubePlayer?,
                wasRestored: Boolean
            ) {
                if (!wasRestored)
                    player?.cueVideo(videoId)

                player?.setPlayerStateChangeListener(object : YouTubePlayer.PlayerStateChangeListener {
                    override fun onAdStarted() {  }
                    override fun onLoading() {  }
                    override fun onVideoStarted() {  }

                    override fun onLoaded(videoId: String?) {
                        player.play()
                    }

                    override fun onVideoEnded() {  }

                    override fun onError(errorReason: YouTubePlayer.ErrorReason?) {
                        showToast(getString(R.string.video_cannot_be_played))
                        Timber.e("$errorReason")
                    }
                })
            }

            override fun onInitializationFailure(
                provider: YouTubePlayer.Provider?,
                result: YouTubeInitializationResult?
            ) {
                showToast(getString(R.string.video_cannot_be_played))
            }

        })
    }

    fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, text, duration).show()
    }

    companion object {
        const val TAG = "YouTubePlayerActivity"
    }
}