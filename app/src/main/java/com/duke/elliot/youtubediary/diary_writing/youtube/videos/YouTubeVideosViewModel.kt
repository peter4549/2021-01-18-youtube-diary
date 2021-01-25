package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.*
import com.duke.elliot.youtubediary.database.youtube.DisplayPlaylistModel
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import com.duke.elliot.youtubediary.database.youtube.NextPageToken
import com.duke.elliot.youtubediary.database.youtube.UpdatedAt
import com.duke.elliot.youtubediary.diary_writing.youtube.*
import com.duke.elliot.youtubediary.util.SimpleDialogFragment
import com.duke.elliot.youtubediary.util.SimpleItem
import com.duke.elliot.youtubediary.util.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

const val KIND_NEXT_PAGE_TOKEN_PLAYLIST = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.kind_next_page_token_playlist"
const val KIND_NEXT_PAGE_TOKEN_VIDEO = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.kind_next_page_token_video"

const val COLLECTION_CHANNEL = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.collection_channel"
const val COLLECTION_PLAYLIST = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.collection_playlist"

const val KIND_UPDATED_AT_CHANNEL = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.kind_updated_at_channel"
const val KIND_UPDATED_AT_PLAYLIST = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.kind_updated_at_playlist"
const val KIND_UPDATED_AT_VIDEO = "com.duke.elliot.youtubediary.diary_writing.youtube.videos.kind_updated_at_video"

class YouTubeVideosViewModel(private val application: Application, val channelId: String): ViewModel() {
    private val database = AppDatabase.getInstance(application)
    private val timeAgoFormatConverter = TimeAgoFormatConverter(application)
    private val blank = ""

    var playlistId: String = YouTubeVideosActivity.DEFAULT_PLAYLIST_ID
    var nextPageToken: String = blank

    /** LiveData */
    var displayPlaylists: MutableLiveData<MutableList<DisplayPlaylistModel>> = MutableLiveData()
    var displayVideos: MutableLiveData<MutableList<DisplayVideoModel>> = MutableLiveData()

    init {
        initDisplayData()
    }

    /**
     * collection: COLLECTION_CHANNEL
     * nextPageToken: (channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST), (channelId, KIND_NEXT_PAGE_TOKEN_VIDEO)
     * updatedAt: (channelId, KIND_UPDATED_AT_CHANNEL), (channelId, KIND_UPDATED_AT_PLAYLIST), (channelId, KIND_UPDATED_AT_VIDEO)
     * add: false
     */
    private fun initDisplayData() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAt = database.updatedAtDao().getUpdatedAt(channelId, KIND_UPDATED_AT_CHANNEL)?.updatedAt

            if (updatedAt.isNotNull()) {
                if (moreThan3HoursPassed(updatedAt)) {
                    /** YouTube Data API */

                    /** Delete existing data. */
                    database.displayPlaylistDao().deleteAll(channelId)
                    database.displayVideoDao().deleteAllByChannelId(channelId)

                    /** Playlist */
                    requestSearchList(channelId, TYPE_PLAYLIST, null).let { searchList ->
                        val nextPageToken = searchList?.nextPageToken ?: blank
                        updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST, nextPageToken)  // Playlist.

                        searchList?.filterKindPlaylist()?.map {
                            createDisplayPlaylistModel(it, channelId)
                        }?.let { displayPlaylists ->
                            /** Insert into local database, Update UI. */
                            updateDisplayPlaylists(displayPlaylists)
                        }

                        updateUpdatedAt(channelId, KIND_UPDATED_AT_PLAYLIST)
                    }

                    /** Video */
                    requestSearchList(channelId, TYPE_VIDEO, null).let { searchList ->
                        val nextPageToken = searchList?.nextPageToken ?: blank
                        updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken) // Video.
                        this@YouTubeVideosViewModel.nextPageToken = nextPageToken

                        searchList?.filterKindVideo()?.map {
                            createDisplayVideoModel(it, collection = COLLECTION_CHANNEL, channelId = channelId)
                        }?.let { displayVideos ->
                            /** Insert into local database, Update UI. */
                            updateDisplayVideos(displayVideos, false)
                        }

                        updateUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)
                    }

                    updateUpdatedAt(channelId, KIND_UPDATED_AT_CHANNEL)
                }  else {
                    /** Local Database */
                    val displayPlaylists = database.displayPlaylistDao().getAllByChannelId(channelId)
                    val displayVideos = database.displayVideoDao().getAllByChannelId(channelId)
                    // Video.
                    nextPageToken = database.nextPageTokenDao().get(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO)?.nextPageToken ?: blank

                    withContext(Dispatchers.Main) {
                        this@YouTubeVideosViewModel.displayPlaylists.value = displayPlaylists
                        this@YouTubeVideosViewModel.displayVideos.value = displayVideos
                    }
                }
            } else {
                /** YouTube Data API */
                /** Playlist */
                requestSearchList(channelId, TYPE_PLAYLIST, null).let { searchList ->
                    val nextPageToken = searchList?.nextPageToken ?: blank
                    updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST, nextPageToken)

                    searchList?.filterKindPlaylist()?.map {
                        createDisplayPlaylistModel(it, channelId)
                    }?.let { displayPlaylists ->
                        /** Insert into local database, Update UI. */
                        updateDisplayPlaylists(displayPlaylists)
                    }

                    insertUpdatedAt(channelId, KIND_UPDATED_AT_PLAYLIST)
                }

                /** Video */
                requestSearchList(channelId, TYPE_VIDEO, null).let { searchList ->
                    val nextPageToken = searchList?.nextPageToken ?: blank
                    updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken)
                    this@YouTubeVideosViewModel.nextPageToken = nextPageToken

                    searchList?.filterKindVideo()?.map {
                        createDisplayVideoModel(it, collection = COLLECTION_CHANNEL, channelId = channelId)
                    }?.let { displayVideos ->
                        /** Insert into local database, Update UI. */
                        updateDisplayVideos(displayVideos, false)
                    }

                    insertUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)
                }

                insertUpdatedAt(channelId, KIND_UPDATED_AT_CHANNEL)
            }
        }
    }

    /**
     * collection: COLLECTION_PLAYLIST
     * nextPageToken: (channelId, KIND_NEXT_PAGE_TOKEN_VIDEO)
     * updatedAt: (playlistId, KIND_UPDATED_AT_VIDEO)
     * add: false
     */
    fun initDisplayVideosByChannelId() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAt = database.updatedAtDao().getUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)?.updatedAt

            if (updatedAt.isNotNull()) {
                if (moreThan3HoursPassed(updatedAt)) {
                    /** YouTube Data API */
                    /** Video */
                    requestSearchList(channelId, TYPE_VIDEO, null).let { searchList ->
                        val nextPageToken = searchList?.nextPageToken ?: blank
                        updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken) // Video.
                        this@YouTubeVideosViewModel.nextPageToken = nextPageToken

                        searchList?.filterKindVideo()?.map {
                            createDisplayVideoModel(it, collection = COLLECTION_CHANNEL, channelId = channelId)
                        }?.let { displayVideos ->
                            /** Insert into local database, Update UI. */
                            updateDisplayVideos(displayVideos, false)
                        }

                        updateUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)
                    }
                }  else {
                    /** Local Database */
                    val displayVideos = database.displayVideoDao().getAllByChannelId(channelId)
                    // Video.
                    nextPageToken = database.nextPageTokenDao().get(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO)?.nextPageToken ?: blank

                    withContext(Dispatchers.Main) {
                        this@YouTubeVideosViewModel.displayVideos.value = displayVideos
                    }
                }
            } else {
                /** YouTube Data API */
                /** Video */
                requestSearchList(channelId, TYPE_VIDEO, null).let { searchList ->
                    val nextPageToken = searchList?.nextPageToken ?: blank
                    updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken)
                    this@YouTubeVideosViewModel.nextPageToken = nextPageToken

                    searchList?.filterKindVideo()?.map {
                        createDisplayVideoModel(it, collection = COLLECTION_CHANNEL, channelId = channelId)
                    }?.let { displayVideos ->
                        /** Insert into local database, Update UI. */
                        updateDisplayVideos(displayVideos, false)
                    }

                    insertUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)
                }
            }
        }
    }

    /**
     * collection: COLLECTION_PLAYLIST
     * nextPageToken: (playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO)
     * updatedAt: (playlistId, KIND_UPDATED_AT_VIDEO)
     * add: false
     */
    fun initDisplayVideosByPlaylistId(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAt = database.updatedAtDao().getUpdatedAt(playlistId, KIND_UPDATED_AT_VIDEO)?.updatedAt

            if (updatedAt.isNotNull()) {
                if (moreThan3HoursPassed(updatedAt)) {
                    /** YouTube Data API */

                    /** Delete existing data. */
                    database.displayVideoDao().deleteAllByPlaylistId(playlistId)

                    val playlistItems = requestPlaylistItems(playlistId, null)

                    /** Update nextPageToken. */
                    nextPageToken = playlistItems?.nextPageToken ?: blank
                    insertNextPageToken(playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken)  // Video.

                    val displayVideoModels = playlistItems?.filterKindVideo()?.map {
                        createDisplayVideoModel(it, collection = COLLECTION_PLAYLIST, playlistId = playlistId)
                    }

                    if (displayVideoModels.isNullOrEmpty())
                        return@launch

                    updateDisplayVideos(displayVideoModels, false)
                    updateUpdatedAt(playlistId, KIND_UPDATED_AT_VIDEO)
                }  else {
                    /** Local Database */
                    val displayVideos = database.displayVideoDao().getAllByPlaylistId(playlistId)
                    nextPageToken = database.nextPageTokenDao()
                        .get(playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO)?.nextPageToken ?: blank

                    withContext(Dispatchers.Main) {
                        this@YouTubeVideosViewModel.displayVideos.value = displayVideos
                    }
                }
            } else {
                /** YouTube Data API */
                val playlistItems = requestPlaylistItems(playlistId, null)

                /** Update nextPageToken. */
                nextPageToken = playlistItems?.nextPageToken ?: blank
                insertNextPageToken(playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO, nextPageToken)

                val displayVideos = playlistItems?.filterKindVideo()?.map {
                    createDisplayVideoModel(it, collection = COLLECTION_PLAYLIST, playlistId = playlistId)
                }

                if (displayVideos.isNullOrEmpty())
                    return@launch

                updateDisplayVideos(displayVideos, false)
                insertUpdatedAt(playlistId, KIND_UPDATED_AT_VIDEO)
            }
        }
    }

    private suspend fun updateDisplayPlaylists(displayPlaylists: List<DisplayPlaylistModel>) {
        database.displayPlaylistDao().insertAll(displayPlaylists)

        withContext(Dispatchers.Main) {
            this@YouTubeVideosViewModel.displayPlaylists.value =
                displayPlaylists as MutableList<DisplayPlaylistModel>
        }
    }

    private suspend fun updateDisplayVideos(displayVideos: List<DisplayVideoModel>, add: Boolean) {
        database.displayVideoDao().insertAll(displayVideos)

        withContext(Dispatchers.Main) {
            if (add) {
                // Call on scroll.
                val value = mutableListOf<DisplayVideoModel>()
                this@YouTubeVideosViewModel.displayVideos.value?.let { value.addAll(it) }
                value.addAll(displayVideos)
                this@YouTubeVideosViewModel.displayVideos.value = value
            } else
                this@YouTubeVideosViewModel.displayVideos.value =
                    displayVideos as MutableList<DisplayVideoModel>
        }
    }

    private suspend fun requestSearchList(channelId: String, type: String, pageToken: String?):
            SearchListModel? = withContext(Dispatchers.IO) {
        try {
            val searchListDeferred = YouTubeApi.searchListService().getSearchListAsync(
                googleApiKey = application.getString(R.string.google_api_key),
                channelId = channelId,
                pageToken = pageToken ?: blank,
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

    /**
     * collection: COLLECTION_CHANNEL
     * nextPageToken: (channelId, KIND_NEXT_PAGE_TOKEN_VIDEO)
     * updatedAt: (channelId, KIND_UPDATED_AT_VIDEO)
     * add: true
     */
    fun addDisplayVideosByChannelId(channelId: String, nextPageToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val searchList = requestSearchList(channelId, TYPE_VIDEO, nextPageToken)
                ?: return@launch

            this@YouTubeVideosViewModel.nextPageToken = searchList.nextPageToken ?: blank
            updateNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_VIDEO, this@YouTubeVideosViewModel.nextPageToken)

            val displayVideoModels = searchList.filterKindVideo().map {
                createDisplayVideoModel(it, collection = COLLECTION_CHANNEL, channelId = channelId)
            }

            /** Insert into local database, Update UI. */
            updateDisplayVideos(displayVideoModels, true)
            updateUpdatedAt(channelId, KIND_UPDATED_AT_VIDEO)
        }
    }

    /**
     * Call on scroll.
     * collection: COLLECTION_PLAYLIST
     * nextPageToken: (playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO)
     * updatedAt: (playlistId, KIND_UPDATED_AT_VIDEO)
     * add: true
     */
    fun addDisplayVideosByPlaylistId(playlistId: String, nextPageToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistItems = requestPlaylistItems(playlistId, nextPageToken)

            this@YouTubeVideosViewModel.nextPageToken = playlistItems?.nextPageToken ?: blank
            updateNextPageToken(playlistId, KIND_NEXT_PAGE_TOKEN_VIDEO, this@YouTubeVideosViewModel.nextPageToken)

            val displayVideos = playlistItems?.filterKindVideo()?.map {
                createDisplayVideoModel(it, collection = COLLECTION_PLAYLIST, playlistId = playlistId)
            }

            this@YouTubeVideosViewModel.displayVideos

            displayVideos?.let { updateDisplayVideos(it, true) }
            updateUpdatedAt(playlistId, KIND_UPDATED_AT_VIDEO)
        }
    }

    /**
     * nextPageToken: (channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST)
     * updatedAt: (channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST)
     */
    fun addDisplayPlaylists(simpleItemAdapter: SimpleDialogFragment.SimpleItemAdapter) {
        viewModelScope.launch(Dispatchers.IO) {
            /** Playlist */
            val nextPageToken = database.nextPageTokenDao()
                .get(channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST)?.nextPageToken

            // Blank should also be checked.
            if (nextPageToken.isNullOrBlank())
                return@launch

            val searchList = requestSearchList(channelId, TYPE_PLAYLIST, nextPageToken)
            val displayPlaylists = searchList?.filterKindPlaylist()?.map {
                createDisplayPlaylistModel(it, channelId)
            }

            /** Insert into local database. */
            displayPlaylists?.let { database.displayPlaylistDao().insertAll(it) }

            this@YouTubeVideosViewModel.nextPageToken = searchList?.nextPageToken ?: blank

            // Use insert.
            insertNextPageToken(channelId, KIND_NEXT_PAGE_TOKEN_PLAYLIST, this@YouTubeVideosViewModel.nextPageToken)

            // Use insert.
            insertUpdatedAt(channelId, KIND_UPDATED_AT_PLAYLIST)

            val simpleItems = displayPlaylists?.map {
                SimpleItem(
                    id = it.id,
                    name = it.title,
                    imageUri = it.thumbnailUri
                )
            } ?: return@launch

            withContext(Dispatchers.Main) {
                simpleItemAdapter.addItems(simpleItems as ArrayList<SimpleItem>)
            }
        }
    }

    private suspend fun requestPlaylistItems(playlistId: String, pageToken: String?):
            PlaylistItemsModel? = withContext(Dispatchers.IO) {
        val playlistItemsDeferred = YouTubeApi.playlistItemsService().getPlaylistItemsAsync(
            googleApiKey = application.getString(R.string.google_api_key),
            playlistId = playlistId,
            pageToken = pageToken ?: blank
        )
        try {
            return@withContext playlistItemsDeferred.await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get videos.")
            return@withContext null
        }
    }

    private fun moreThan3HoursPassed(updatedAt: Long?): Boolean {
        val hours = (System.currentTimeMillis() - (updatedAt ?: 0L)) / (1000 * 60 * 60).toLong()
        return hours >= 3L
    }

    private fun insertNextPageToken(id: String, kind: String, nextPageToken: String) {
        database.nextPageTokenDao().insert(
            NextPageToken(
                id = id,
                kind = kind,
                nextPageToken = nextPageToken
            )
        )
    }

    private fun updateNextPageToken(id: String, kind: String, nextPageToken: String) {
        database.nextPageTokenDao().insert(
            NextPageToken(
                id = id,
                kind = kind,
                nextPageToken = nextPageToken
            )
        )
    }

    private fun insertUpdatedAt(id: String, kind: String) {
        database.updatedAtDao().insert(
            UpdatedAt(
                id = id,
                kind = kind,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun updateUpdatedAt(id: String, kind: String) {
        database.updatedAtDao().update(
            UpdatedAt(
                id = id,
                kind = kind,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(application, text, duration).show()
    }

    private fun createDisplayVideoModel(
        video: VideoModel,
        collection: String,
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
            collection = collection
        )
    }

    private fun createDisplayPlaylistModel(playlist: PlaylistModel, channelId: String): DisplayPlaylistModel {
        var thumbnailUri = playlist.snippet.thumbnails.maxresModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlist.snippet.thumbnails.standard?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlist.snippet.thumbnails.highModel?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlist.snippet.thumbnails.medium?.url
        if (thumbnailUri.isNullOrBlank())
            thumbnailUri = playlist.snippet.thumbnails.default.url

        return DisplayPlaylistModel(
            id = playlist.id,
            channelId = channelId,
            title = playlist.snippet.title,
            description = playlist.snippet.description,
            thumbnailUri = thumbnailUri
        )
    }
}