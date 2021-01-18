package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
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


class YouTubeVideosActivity: BaseActivity(), VideoAdapter.OnMenuItemClickListener {

    private lateinit var binding:ActivityYoutubeVideosBinding
    private lateinit var viewModel: YouTubeVideosViewModel
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var timeAgoFormatConverter: TimeAgoFormatConverter
    private lateinit var videoAdapter: VideoAdapter

    private var uninitialized = true
    private var previousPageToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_youtube_videos)

        val viewModelFactory = YouTubeVideosViewModelFactory()
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[YouTubeVideosViewModel::class.java]
        viewModel.playlistId = DEFAULT_PLAYLIST_ID

        viewModel.channelId = intent.getStringExtra(EXTRA_NAME_CHANNEL_ID)
        timeAgoFormatConverter = TimeAgoFormatConverter(this)

        binding.frameLayoutPlaylist.setOnClickListener {
            showPlaylistSelectionDialogFragment(viewModel.displayPlaylists)
        }

        binding.textPlaylist.text = getString(R.string.all_videos)

        initRecyclerView()

        viewModel.channelId?.let {
            getSearchList(it, null)
        } ?: run {
            showToast(getString(R.string.channel_not_found))
            finish()
        }
    }

    private fun getSearchList(channelId: String, pageToken: String?) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val searchListDeferred = YouTubeApi.searchListService().getSearchListAsync(
                    googleApiKey = getString(R.string.google_api_key),
                    channelId = channelId,
                    pageToken = pageToken ?: ""
                )
                try {
                    val searchList = searchListDeferred.await()
                    val nextPageToken = searchList.nextPageToken
                    viewModel.playlistIdNextPageTokenMap[DEFAULT_PLAYLIST_ID] = nextPageToken
                    getVideosFromSearchList(searchList)

                    if (uninitialized) {
                        getPlaylistsFromSearchList(searchList)
                        uninitialized = false
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get search list.")
                }
            }
        }
    }

    private fun getVideosFromSearchList(searchList: SearchListModel) {
        val videos = searchList.items.filter { it.id.kind == KIND_VIDEO }.map {
            VideoModel (
                id = it.id.videoId ?: "",
                snippet = it.snippet,
                statistics = null
            )
        }

        coroutineScope.launch {
            val displayVideos = videos.map { createDisplayVideoModel(it) } as ArrayList
            videoAdapter.addAll(displayVideos)
        }
    }

    private fun getVideosByPlaylistId(playlistId: String, pageToken: String?) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val playlistItemsDeferred = YouTubeApi.playlistItemsService().getPlaylistItemsAsync(
                    googleApiKey = getString(R.string.google_api_key),
                    playlistId = playlistId,
                    pageToken = pageToken ?: ""
                )
                try {
                    val playlistItems = playlistItemsDeferred.await()
                    val nextPageToken = playlistItems.nextPageToken
                    viewModel.playlistIdNextPageTokenMap[playlistId] = nextPageToken

                    val videos = playlistItems.items.filter { it.snippet.resourceId.kind == KIND_VIDEO }.map {
                        VideoModel(
                            id = it.snippet.resourceId.videoId,
                            snippet = it.snippet,
                            statistics = null
                        )
                    }

                    val displayVideos = videos.map {
                        createDisplayVideoModel(it)
                    } as ArrayList

                    coroutineScope.launch {
                        videoAdapter.addAll(displayVideos)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get videos.")
                }
            }
        }
    }

    private fun getPlaylistsFromSearchList(searchList: SearchListModel) {
        val playlists = searchList.items.filter { it.id.kind == KIND_PLAYLIST }.map {
            PlaylistModel(
                id = it.id.playlistId ?: "",
                snippet = it.snippet
            )
        }

        val displayPlaylists = playlists.map {
            createDisplayPlaylistModel(it)
        } as ArrayList

        viewModel.displayPlaylists = displayPlaylists
    }

    private fun createDisplayPlaylistModel(playlistModel: PlaylistModel): DisplayPlaylistModel {
        var thumbnailUri = playlistModel.snippet.thumbnails.maxresModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlistModel.snippet.thumbnails.standard?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlistModel.snippet.thumbnails.highModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlistModel.snippet.thumbnails.medium?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlistModel.snippet.thumbnails.default.url

        return DisplayPlaylistModel(
            id = playlistModel.id,
            title = playlistModel.snippet.title,
            description = playlistModel.snippet.description,
            thumbnailUri = thumbnailUri
        )
    }

    private fun createDisplayVideoModel(video: VideoModel): DisplayVideoModel {
        val id = video.id
        var thumbnailUri = video.snippet.thumbnails.maxresModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = video.snippet.thumbnails.standard?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = video.snippet.thumbnails.highModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = video.snippet.thumbnails.medium?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = video.snippet.thumbnails.default.url
        val title = video.snippet.title
        val publishedAt = video.snippet.publishedAt

        return DisplayVideoModel(
            id = id,
            thumbnailUri = thumbnailUri,
            title = title,
            timeAgo = timeAgoFormatConverter.covertToTimeAgoFormat(publishedAt) ?: ""
        )
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
                    if (viewModel.playlistId.isNullOrBlank()) {
                        Timber.e(NullPointerException("playlistId is null."))
                        showToast(getString(R.string.playlist_not_found))
                        return
                    }

                    val pageToken = viewModel.playlistIdNextPageTokenMap[viewModel.playlistId]

                    if (pageToken.isNullOrBlank())
                        return

                    if (previousPageToken == pageToken)
                        return

                    previousPageToken = pageToken

                    if (viewModel.playlistId == DEFAULT_PLAYLIST_ID)
                        viewModel.channelId?.let { channelId ->
                            getSearchList(channelId, pageToken)
                        }
                    else {
                        viewModel.playlistId?.let { getVideosByPlaylistId(it, pageToken) }
                    }
                }
            }
        })
    }

    private fun showPlaylistSelectionDialogFragment(playlists: List<DisplayPlaylistModel>) {
        if (playlists.isNullOrEmpty()) {
            showToast(getString(R.string.playlist_not_found))
            return
        }

        val simpleDialogFragment = SimpleDialogFragment()
        simpleDialogFragment.setTitle(getString(R.string.playlist))

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

        simpleDialogFragment.setItems(items)
        simpleDialogFragment.setOnItemSelectedListener { dialogFragment, simpleItem ->
            dialogFragment.dismiss()

            val playlistId = simpleItem.id

            if (viewModel.playlistId != playlistId) {
                viewModel.playlistId = playlistId
                previousPageToken = null
                videoAdapter.clear()
                binding.textPlaylist.text = simpleItem.name

                if (playlistId == DEFAULT_PLAYLIST_ID)
                    viewModel.channelId?.let {
                        getSearchList(it, null)
                    } ?: run {
                        showToast(getString(R.string.channel_not_found))
                    }
                else
                    getVideosByPlaylistId(simpleItem.id, null)
            }
        }
        simpleDialogFragment.show(supportFragmentManager, simpleDialogFragment.tag)
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

        private const val DEFAULT_PLAYLIST_ID = "com.duke.elliot.youtubediary.diary_writing.youtube.videos" +
                ".youtube_videos_activity.default_playlist_id"
    }
}