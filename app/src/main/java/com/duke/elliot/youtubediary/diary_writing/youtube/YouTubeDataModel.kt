package com.duke.elliot.youtubediary.diary_writing.youtube

const val KIND_CHANNEL = "youtube#channel"
const val KIND_PLAYLIST = "youtube#playlist"
const val KIND_VIDEO = "youtube#video"

data class AccessTokenModel(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String
)

data class ChannelsModel(val items: List<ItemModel>)

data class PlaylistItemsModel (
    val kind: String,
    val etag: String,
    val nextPageToken: String?,
    val items: List<ItemModel>
) {
    fun filterKindVideo() = items.filter {
        it.snippet.resourceId.kind == KIND_VIDEO
    }.map {
        VideoModel(
            id = it.snippet.resourceId.videoId,
            snippet = it.snippet,
            // statistics = null  // Unused.
        )
    }
}

data class PlaylistsModel(val kind: String,
                          val etag: String,
                          val nextPageToken: String?,
                          val items: List<ItemModel>)

data class PlaylistModel(
    val id: String,
    val snippet: SnippetModel
)

data class SearchListModel(
    val kind: String,
    /** val etag: String, */
    /** val regionCode: String, */
    val nextPageToken: String?,
    val items: List<SearchResultModel>
) {

    fun filterKindPlaylist() = items.filter {
        it.id.kind == KIND_PLAYLIST
    }.map {
        PlaylistModel(
            id = it.id.playlistId ?: "",
            snippet = it.snippet
        )
    }

    fun filterKindVideo() = items.filter {
        it.id.kind == KIND_VIDEO
    }.map {
        VideoModel (
            id = it.id.videoId ?: "",
            snippet = it.snippet,
            // statistics = null  // Unused.
        )
    }
}

data class SearchResultModel(
    val kind: String,
    /** val etag: String, */
    val id: SearchResultIdModel,
    val snippet: SnippetModel
)

data class SearchResultIdModel(
    val kind: String,
    val channelId: String?,
    val playlistId: String?,
    val videoId: String?,
)

data class VideosModel(val nextPageToken: String,
                       val items: List<VideoModel>)

data class VideoModel(
    val id: String,
    val snippet: SnippetModel,
    /** val statistics: StatisticsModel? */
)

data class ItemModel(val kind: String,
                     val etag: String,
                     val id: String,
                     val contentDetails: ContentDetailsModel,
                     val snippet: SnippetModel,
                     /** val statistics: StatisticsModel */ )

data class ResourceIdModel(
    val kind: String,
    val videoId: String
)

data class ContentDetailsModel(val itemCount: Int,
                               val videoId: String,
                               val videoPublishedAt: String)

data class SnippetModel(val publishedAt: String,
                        val channelId: String,
                        val title: String,
                        val description: String,
                        val thumbnails: ThumbnailsModel,
                        val resourceId: ResourceIdModel)

data class ThumbnailsModel(val default: DefaultModel,
                           val medium: MediumModel?,
                           val standard: StandardModel?,
                           val highModel: HighModel?,
                           val maxresModel: MaxresModel?)

data class DefaultModel(val url: String,
        /** val width: Int, */
        /** val height: Int */ )

data class MediumModel(val url: String,
        /** val width: Int, */
        /** val height: Int */ )

data class HighModel(
        val url: String,
        /** val width: Int, */
        /** val height: Int */
)

// Playlists, Videos
data class StandardModel(
        val url: String,
        /** val width: Int, */
        /** val height: Int */
)

@Suppress("SpellCheckingInspection")
data class MaxresModel(
        val url: String,
        /** val width: Int, */
        /** val height: Int */
)

data class StatisticsModel(val viewCount: String,
                           val likeCount: String,
                           val dislikeCount: String,
                           val favoriteCount: String,
                           val commentCount: String)