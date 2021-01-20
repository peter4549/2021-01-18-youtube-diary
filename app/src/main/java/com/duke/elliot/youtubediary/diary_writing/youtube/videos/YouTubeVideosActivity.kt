package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.DisplayPlaylistModel
import com.duke.elliot.youtubediary.database.DisplayVideoModel
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
        viewModel.displayPlaylistModels.observe(this, { displayPlaylistModels ->
            if (displayPlaylistModels.isEmpty()) {

            } else {
                initPlaylistSelectionDialogFragment(displayPlaylistModels)
            }
        })

        viewModel.displayVideoModels.observe(this, { displayVideoModels ->
            if (displayVideoModels.isNotEmpty()) {
                videoAdapter.submitList(displayVideoModels as ArrayList<DisplayVideoModel>)
            }
        })
    }

    /*
    private fun getSearchList(channelId: String, type: String, pageToken: String?) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val searchListDeferred = YouTubeApi.searchListService().getSearchListAsync(
                    googleApiKey = getString(R.string.google_api_key),
                    channelId = channelId,
                    pageToken = pageToken ?: "",
                    type = type
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

     */

    /* TODO: check here.
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
                            // statistics = null
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

     */

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
                    //  NextPageToken is updated when the page is loaded.
                    val pageToken = viewModel.playlistIdNextPageTokenMap[viewModel.playlistId]
                    if (pageToken.isNullOrBlank())
                        return

                    if (previousPageToken == pageToken)
                        return

                    previousPageToken = pageToken

                    showToast("AAA: $pageToken")

                    if (viewModel.playlistId == SEARCH_LIST_NEXT_PAGE_TOKEN_KEY)
                        viewModel.addDisplayVideoModelsByChannelId(viewModel.channelId, pageToken)
                    else
                        viewModel.addDisplayVideoModelsByPlaylistId(viewModel.playlistId, pageToken)
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
            id = SEARCH_LIST_NEXT_PAGE_TOKEN_KEY,
            name = getString(R.string.all_videos),
        ))

        simpleDialogFragment?.setItems(items)
        simpleDialogFragment?.setOnItemSelectedListener { dialogFragment, simpleItem ->
            dialogFragment.dismiss()

            val playlistId = simpleItem.id

            if (viewModel.playlistId != playlistId) {
                viewModel.playlistId = playlistId
                previousPageToken = null
                videoAdapter.clear()
                binding.textPlaylist.text = simpleItem.name

                if (playlistId == SEARCH_LIST_NEXT_PAGE_TOKEN_KEY)
                    viewModel.initDisplayDataModels() // TODO change only update video.
                else
                    viewModel.initDisplayVideoDataModelsByPlaylistId(simpleItem.id)
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

        const val SEARCH_LIST_NEXT_PAGE_TOKEN_KEY = "com.duke.elliot.youtubediary.diary_writing.youtube.videos" +
                ".youtube_videos_activity.search_list_next_page_token_key"
    }

    /** Load more playlists */
    override fun onScrollReachedBottom(simpleItemAdapter: SimpleDialogFragment.SimpleItemAdapter) {
        viewModel.addDisplayPlaylists(simpleItemAdapter)
    }
}