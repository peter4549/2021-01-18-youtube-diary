package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.*
import com.duke.elliot.youtubediary.diary_writing.youtube.*
import com.duke.elliot.youtubediary.util.SimpleDialogFragment
import com.duke.elliot.youtubediary.util.SimpleItem
import com.duke.elliot.youtubediary.util.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

class YouTubeVideosViewModel(private val application: Application, val channelId: String): ViewModel() {
    private val database = YouTubeDatabase.getInstance(application)
    private val timeAgoFormatConverter = TimeAgoFormatConverter(application)

    var playlistId: String = YouTubeVideosActivity.SEARCH_LIST_NEXT_PAGE_TOKEN_KEY  // Initial value.
    var playlistNextPageToken: String? = null
    val playlistIdNextPageTokenMap = mutableMapOf<String?, String?>()

    var displayPlaylistModels: MutableLiveData<MutableList<DisplayPlaylistModel>> = MutableLiveData()
    var displayVideoModels: MutableLiveData<MutableList<DisplayVideoModel>> = MutableLiveData()

    val blank = ""

    init {
        initDisplayDataModels()
    }

    fun initDisplayDataModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAt = database.updatedAtDao().getUpdatedAt(channelId, KIND_CHANNEL)?.updatedAt

            if (updatedAt.isNotNull()) {
                if (moreThan3HoursPassed(updatedAt)) {
                    /** YouTube Data API */
                    val searchListTypePlaylist = getSearchList(channelId, SEARCH_LIST_TYPE_PLAYLIST, null)
                    val searchListTypeVideo = getSearchList(channelId, SEARCH_LIST_TYPE_VIDEO, null)

                    playlistIdNextPageTokenMap[YouTubeVideosActivity.SEARCH_LIST_NEXT_PAGE_TOKEN_KEY] = searchListTypeVideo?.nextPageToken
                    insertNextPageToken(channelId, KIND_CHANNEL, searchListTypeVideo?.nextPageToken ?: blank)
                    searchListTypePlaylist?.nextPageToken?.let {
                        insertPlaylistNextPageToken(it)
                        playlistNextPageToken = it
                    }

                    val displayPlaylistModels = searchListTypePlaylist?.let {
                        getDisplayPlaylistModelsFromSearchList(it)
                    }

                    val displayVideoModels = searchListTypeVideo?.let {
                        getDisplayVideoModelsFromSearchList(it)
                    }

                    updateYouTubeDatabase(displayPlaylistModels, displayVideoModels)
                    updateUI(displayPlaylistModels, displayVideoModels)

                    database.updatedAtDao().update(
                        UpdatedAt(
                            id = channelId,
                            updatedAt = System.currentTimeMillis(),
                            kind = KIND_CHANNEL
                        )
                    )
                }  else {
                    /** Local Database */
                    val displayPlaylists = database.displayPlaylistDao().getAll(channelId)
                    val displayVideos = database.displayVideoDao().getAllByChannelId(channelId)
                    playlistIdNextPageTokenMap[YouTubeVideosActivity.SEARCH_LIST_NEXT_PAGE_TOKEN_KEY] = getNextPageTokenByChannelId()
                    playlistNextPageToken = getPlaylistNextPageToken()?.nextPageToken
                    updateUI(displayPlaylists, displayVideos)
                }
            } else {
                /** YouTube Data API */
                val searchListTypePlaylist = getSearchList(channelId, SEARCH_LIST_TYPE_PLAYLIST, null)
                val searchListTypeVideo = getSearchList(channelId, SEARCH_LIST_TYPE_VIDEO, null)
                playlistIdNextPageTokenMap[YouTubeVideosActivity.SEARCH_LIST_NEXT_PAGE_TOKEN_KEY] = searchListTypeVideo?.nextPageToken
                insertNextPageToken(channelId, KIND_CHANNEL, searchListTypeVideo?.nextPageToken ?: blank)
                searchListTypePlaylist?.nextPageToken?.let {
                    insertPlaylistNextPageToken(it)
                    playlistNextPageToken = it
                }

                val displayPlaylistModels = searchListTypePlaylist?.let {
                    getDisplayPlaylistModelsFromSearchList(it)
                }
                val displayVideoModels = searchListTypeVideo?.let {
                    getDisplayVideoModelsFromSearchList(it)
                }

                updateYouTubeDatabase(displayPlaylistModels, displayVideoModels)
                updateUI(displayPlaylistModels, displayVideoModels)

                database.updatedAtDao().insert(
                    UpdatedAt(
                        id = channelId,
                        updatedAt = System.currentTimeMillis(),
                        kind = KIND_CHANNEL
                    )
                )
            }
        }
    }

    fun initDisplayVideoDataModelsByPlaylistId(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAt = database.updatedAtDao().getUpdatedAt(playlistId, KIND_PLAYLIST)?.updatedAt
            if (updatedAt.isNotNull()) {
                if (moreThan3HoursPassed(updatedAt)) {
                    /** YouTube Data API */
                    // NextPageToken is also updated.
                    val displayVideoModels = getVideosByPlaylistId(playlistId, null)

                    if (displayVideoModels.isNullOrEmpty())
                        return@launch

                    updateYouTubeDatabase(null, displayVideoModels)
                    updateUI(null, displayVideoModels)

                    database.updatedAtDao().update(
                        UpdatedAt(
                            id = playlistId,
                            updatedAt = System.currentTimeMillis(),
                            kind = KIND_PLAYLIST
                        )
                    )
                }  else {
                    /** Local Database */
                    val displayVideoModels = database.displayVideoDao().getAllByPlaylistId(playlistId)
                    val pageToken = database.nextPageTokenDao()
                        .getNextPageToken(playlistId, KIND_PLAYLIST)?.nextPageToken
                    playlistIdNextPageTokenMap[playlistId] = pageToken
                    updateUI(null, displayVideoModels)
                }
            } else {
                /** YouTube Data API */
                // NextPageToken is also updated.
                val displayVideoModels = getVideosByPlaylistId(playlistId, null)

                if (displayVideoModels.isNullOrEmpty())
                    return@launch

                updateYouTubeDatabase(null, displayVideoModels)
                updateUI(null, displayVideoModels)

                database.updatedAtDao().insert(
                    UpdatedAt(
                        id = playlistId,
                        updatedAt = System.currentTimeMillis(),
                        kind = KIND_PLAYLIST
                    )
                )
            }
        }
    }

    private fun updateYouTubeDatabase (
        displayPlaylistModels: List<DisplayPlaylistModel>?,
        displayVideoModels: List<DisplayVideoModel>?
    ) {
        displayPlaylistModels?.let { database.displayPlaylistDao().insertAll(it) }
        displayVideoModels?.let { database.displayVideoDao().insertAll(it) }
    }

    private suspend fun updateUI (
        displayPlaylistModels: List<DisplayPlaylistModel>?,
        displayVideoModels: List<DisplayVideoModel>?
    ) {
        withContext(Dispatchers.Main) {
            displayPlaylistModels?.let {
                this@YouTubeVideosViewModel.displayPlaylistModels.value =
                    displayPlaylistModels as MutableList<DisplayPlaylistModel>
            }

            displayVideoModels?.let {
                this@YouTubeVideosViewModel.displayVideoModels.value =
                    displayVideoModels as MutableList<DisplayVideoModel>
            }
        }
    }

    private suspend fun getSearchList(channelId: String, type: String, pageToken: String?): SearchListModel? =
        withContext(Dispatchers.IO){
            try {
                val searchListDeferred = YouTubeApi.searchListService().getSearchListAsync(
                    googleApiKey = application.getString(R.string.google_api_key),
                    channelId = channelId,
                    pageToken = pageToken ?: "",
                    type = type
                )

                searchListDeferred.await()
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    showToast(application.getString(R.string.video_not_found))
                }
                null
            }
        }

    private fun getDisplayVideoModelsFromSearchList(searchList: SearchListModel): List<DisplayVideoModel> {
        val videos = searchList.items.filter { it.id.kind == KIND_VIDEO }.map {
            VideoModel (
                id = it.id.videoId ?: "",
                snippet = it.snippet,
                // statistics = null  // unused.
            )
        }

        return videos.map {
            createDisplayVideoModel(it, kind = KIND_CHANNEL, channelId = channelId)
        }
    }

    private fun getDisplayPlaylistModelsFromSearchList(searchList: SearchListModel): List<DisplayPlaylistModel> {
        val playlists = searchList.items.filter { it.id.kind == KIND_PLAYLIST }.map {
            PlaylistModel(
                id = it.id.playlistId ?: "",
                snippet = it.snippet
            )
        }

        return playlists.map {
            createDisplayPlaylistModel(it, channelId)
        }
    }

    fun addDisplayVideoModelsByChannelId(channelId: String, nextPageToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val searchList = getSearchList(channelId, SEARCH_LIST_TYPE_PLAYLIST_VIDEO, nextPageToken)
                ?: return@launch

            playlistIdNextPageTokenMap[YouTubeVideosActivity.SEARCH_LIST_NEXT_PAGE_TOKEN_KEY] = searchList.nextPageToken
            insertNextPageToken(channelId, KIND_CHANNEL, searchList.nextPageToken ?: blank)

            val displayVideoModels = getDisplayVideoModelsFromSearchList(searchList)

            updateYouTubeDatabase(null, displayVideoModels)
            updateUI(null, displayVideoModels)

            database.updatedAtDao().update(
                UpdatedAt(
                    id = channelId,
                    updatedAt = System.currentTimeMillis(),
                    kind = KIND_CHANNEL
                )
            )
        }
    }

    fun addDisplayVideoModelsByPlaylistId(playlistId: String, nextPageToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // NextPageToken is also updated in getVideosByPlaylistId.
            val displayVideoModels = getVideosByPlaylistId(playlistId, nextPageToken)

            updateYouTubeDatabase(null, displayVideoModels)
            updateUI(null, displayVideoModels)

            database.updatedAtDao().update(
                UpdatedAt(
                    id = playlistId,
                    updatedAt = System.currentTimeMillis(),
                    kind = KIND_PLAYLIST
                )
            )
        }
    }

    private suspend fun getVideosByPlaylistId(playlistId: String, pageToken: String?):
            List<DisplayVideoModel>? = withContext(Dispatchers.IO) {
        val playlistItemsDeferred = YouTubeApi.playlistItemsService().getPlaylistItemsAsync(
            googleApiKey = application.getString(R.string.google_api_key),
            playlistId = playlistId,
            pageToken = pageToken ?: ""
        )
        try {
            val playlistItems = playlistItemsDeferred.await()
            val nextPageToken = playlistItems.nextPageToken
            // Update next page token.
            playlistIdNextPageTokenMap[playlistId] = nextPageToken
            insertNextPageToken(playlistId, KIND_PLAYLIST, nextPageToken ?: blank)

            val videos = playlistItems.items.filter { it.snippet.resourceId.kind == KIND_VIDEO }.map {
                VideoModel(
                    id = it.snippet.resourceId.videoId,
                    snippet = it.snippet,
                    // statistics = null
                )
            }

            return@withContext videos.map {
                createDisplayVideoModel(it, kind = KIND_PLAYLIST, playlistId = playlistId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get videos.")
            return@withContext null
        }
    }

    private fun moreThan3HoursPassed(updatedAt: Long?): Boolean {
        val hours = (System.currentTimeMillis() - (updatedAt ?: 0L)) / (1000 * 60 * 60).toLong()
        return hours >= 1L // TODO: up to 3, 1 is for test.
    }

    fun insertNextPageToken(id: String, kind: String, nextPageToken: String) {
        database.nextPageTokenDao().insert(
            NextPageToken(
                id = id,
                kind = kind,
                nextPageToken = nextPageToken
            )
        )
    }

    private fun getNextPageTokenByChannelId(): String? {
        return database.nextPageTokenDao().getNextPageToken(
            id = channelId,
            kind = KIND_CHANNEL
        )?.nextPageToken
    }

    fun insertPlaylistNextPageToken(nextPageToken: String) {
        database.playlistNextPageTokenDao().insert(
            PlaylistNextPageToken(
                channelId = channelId,
                nextPageToken = nextPageToken
            )
        )
    }

    private fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(application, text, duration).show()
    }

    fun addDisplayPlaylists(simpleItemAdapter: SimpleDialogFragment.SimpleItemAdapter) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playlistNextPageToken.isNullOrBlank())
                return@launch

            val searchListTypePlaylist = getSearchList(channelId, SEARCH_LIST_TYPE_PLAYLIST, playlistNextPageToken) ?: return@launch
            val nextPageToken = searchListTypePlaylist.nextPageToken
            nextPageToken?.let { insertPlaylistNextPageToken(it) }
            playlistNextPageToken = nextPageToken

            val displayPlaylistModels = getDisplayPlaylistModelsFromSearchList(searchListTypePlaylist)
            updateYouTubeDatabase(displayPlaylistModels, null)

            val simpleItems = displayPlaylistModels.map {
                SimpleItem(
                    id = it.id,
                    name = it.title,
                    imageUri = it.thumbnailUri
                )
            } as ArrayList

            withContext(Dispatchers.Main) {
                simpleItemAdapter.addItems(simpleItems)
            }
        }
    }

    private fun getPlaylistNextPageToken() = database.playlistNextPageTokenDao().getPlaylistNextPageToken(channelId)

    private fun createDisplayVideoModel(
        video: VideoModel,
        kind: String,
        channelId: String? = null,
        playlistId: String? = null
    ): DisplayVideoModel {

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
            timeAgo = timeAgoFormatConverter.covertToTimeAgoFormat(publishedAt) ?: "",
            channelId = channelId,
            playlistId = playlistId,
            kind = kind
        )
    }

    private fun createDisplayPlaylistModel(playlistModel: PlaylistModel, channelId: String): DisplayPlaylistModel {
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
            channelId = channelId,
            title = playlistModel.snippet.title,
            description = playlistModel.snippet.description,
            thumbnailUri = thumbnailUri
        )
    }
}