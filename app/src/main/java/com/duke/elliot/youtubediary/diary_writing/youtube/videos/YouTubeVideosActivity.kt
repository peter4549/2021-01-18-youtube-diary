package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.youtube.DisplayPlaylistModel
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ActivityYoutubeVideosBinding
import com.duke.elliot.youtubediary.diary_writing.youtube.*
import com.duke.elliot.youtubediary.diary_writing.youtube.channels.YouTubeChannelsActivity.Companion.EXTRA_NAME_CHANNEL_ID
import com.duke.elliot.youtubediary.diary_writing.youtube.player.YouTubePlayerActivity
import com.duke.elliot.youtubediary.util.SimpleDialogFragment
import com.duke.elliot.youtubediary.util.SimpleItem
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class YouTubeVideosActivity: BaseActivity(), VideoAdapter.OnMenuItemClickListener, SimpleDialogFragment.OnScrollReachedBottomListener {

    private lateinit var binding:ActivityYoutubeVideosBinding
    private lateinit var viewModel: YouTubeVideosViewModel
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var timeAgoFormatConverter: TimeAgoFormatConverter
    private lateinit var videoAdapter: VideoAdapter

    private var uninitialized = true
    private var previousPageToken: String? = null

    private var simpleDialogFragment: SimpleDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_youtube_videos)

        val channelId = intent.getStringExtra(EXTRA_NAME_CHANNEL_ID) ?: ""

        if (channelId.isBlank()) {
            showToast(getString(R.string.channel_not_found))
            finish()
        }

        val viewModelFactory = YouTubeVideosViewModelFactory(application, channelId)
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[YouTubeVideosViewModel::class.java]

        timeAgoFormatConverter = TimeAgoFormatConverter(this)

        binding.frameLayoutPlaylist.setOnClickListener {
            simpleDialogFragment?.show(supportFragmentManager, simpleDialogFragment?.tag)
        }

        binding.textPlaylist.text = getString(R.string.all_videos)

        initRecyclerView()

        /** LiveData */
        viewModel.displayPlaylists.observe(this, { displayPlaylistModels ->
            initPlaylistSelectionDialogFragment(displayPlaylistModels) // TODO 비이었는 경우 처리. 유저에게 알리기.
        })

        viewModel.displayVideos.observe(this, { displayVideos ->
            if (displayVideos.isNotEmpty()) {
                videoAdapter.submitList(displayVideos) // TODO 비어있는 경우 유저에게 알리기. 락 풀리면 테스트.
            }
        })
    }

    private fun initRecyclerView() {
        videoAdapter = VideoAdapter()
        videoAdapter.setOnMenuItemClickListener(this)
        binding.recyclerViewVideo.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@YouTubeVideosActivity)
            adapter = videoAdapter
        }

        binding.recyclerViewVideo.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                Timber.d("recyclerViewVideo scrolled. dx: $dx, dy: $dy")
                val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
                val itemCount = layoutManager.itemCount
                val lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()

                if (lastCompletelyVisibleItemPosition >= itemCount.dec()) {
                    val pageToken = viewModel.nextPageToken

                    if (pageToken.isBlank())
                        return

                    if (previousPageToken == pageToken)
                        return

                    previousPageToken = pageToken

                    showToast("AAA: $pageToken")

                    if (viewModel.playlistId == DEFAULT_PLAYLIST_ID)
                        viewModel.addDisplayVideosByChannelId(viewModel.channelId, pageToken)
                    else
                        viewModel.addDisplayVideosByPlaylistId(viewModel.playlistId, pageToken)
                }
            }
        })
    }

    private fun initPlaylistSelectionDialogFragment(playlists: List<DisplayPlaylistModel>) {
        if (playlists.isNullOrEmpty()) {
            showToast(getString(R.string.playlist_not_found))
            return
        }

        simpleDialogFragment?.clear()
        simpleDialogFragment = SimpleDialogFragment()
        simpleDialogFragment?.setTitle(getString(R.string.playlist))

        val items = playlists.map {
            SimpleItem (
                    id = it.id,
                    name = it.title,
                    imageUri = it.thumbnailUri
            )
        } as ArrayList<SimpleItem>

        items.add(0,  SimpleItem(
            id = DEFAULT_PLAYLIST_ID,
            name = getString(R.string.all_videos),
        ))

        simpleDialogFragment?.setItems(items)
        simpleDialogFragment?.setOnItemSelectedListener { dialogFragment, simpleItem ->
            dialogFragment.dismiss()

            val playlistId = simpleItem.id

            if (viewModel.playlistId != playlistId) {
                viewModel.playlistId = playlistId
                previousPageToken = null
                binding.textPlaylist.text = simpleItem.name

                if (playlistId == DEFAULT_PLAYLIST_ID)
                    viewModel.initDisplayVideosByChannelId()
                else
                    viewModel.initDisplayVideosByPlaylistId(simpleItem.id)
            }
        }

        simpleDialogFragment?.setOnScrollReachedBottomListener(this)
    }

    override fun play(displayVideoModel: DisplayVideoModel) {
        val videoId = displayVideoModel.id
        val intent = Intent(this, YouTubePlayerActivity::class.java)
        intent.putExtra(EXTRA_NAME_VIDEO_ID, videoId)
        startActivity(intent)
    }

    override fun addToDiary(displayVideoModel: DisplayVideoModel) {
        val intent = Intent()
        intent.putExtra(EXTRA_NAME_DISPLAY_VIDEO_MODEL, displayVideoModel)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_NAME_DISPLAY_VIDEO_MODEL = "com.duke.elliot.youtubediary.diary_writing.youtube.videos" +
                ".youtube_videos_activity.extra_name_display_video_model"
        const val EXTRA_NAME_VIDEO_ID = "com.duke.elliot.youtubediary.diary_writing.youtube.videos" +
                ".youtube_videos_activity.extra_name_video_id"
        const val DEFAULT_PLAYLIST_ID = "com.duke.elliot.youtubediary.diary_writing.youtube.videos" +
                ".youtube_videos_activity.default_playlist_id"
    }

    /** Load more playlists. */
    override fun onScrollReachedBottom(simpleItemAdapter: SimpleDialogFragment.SimpleItemAdapter) {
        viewModel.addDisplayPlaylists(simpleItemAdapter)
    }
}