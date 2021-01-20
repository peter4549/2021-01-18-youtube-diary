package com.duke.elliot.youtubediary.diary_writing.youtube

import com.google.gson.internal.LinkedTreeMap
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

const val SEARCH_LIST_TYPE_PLAYLIST = "playlist"
const val SEARCH_LIST_TYPE_VIDEO = "video"
const val SEARCH_LIST_TYPE_PLAYLIST_VIDEO = "playlist,video"

object YouTubeApi {
    const val GOOGLE_AUTHORIZATION_SERVER_URL = "https://accounts.google.com/o/oauth2/token"
    const val ANDROID_CLIENT_ID = "203541998677-2dlo08se98s2ickum70eg1617d5a03gj.apps.googleusercontent.com"
    const val REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
    private const val RESPONSE_TYPE = "code"
    private const val SCOPE = "https://www.googleapis.com/auth/youtube"
    private const val ACCESS_TYPE = "offline"

    const val googleAuthorizationServerUrl = "https://accounts.google.com/o/oauth2/auth?" +
            "client_id=$ANDROID_CLIENT_ID&" +
            "redirect_uri=$REDIRECT_URI&" +
            "scope=$SCOPE&" +
            "response_type=$RESPONSE_TYPE&" +
            "access_type=$ACCESS_TYPE"

    private fun createRetrofit(): Retrofit {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })

        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(builder.build())
            .build()
    }

    interface ChannelsService {
        @GET("/youtube/v3/channels")
        fun getChannelsAsync(
            @Header("Authorization") Authorization: String,
            @Query("part") part: String = "id,snippet",
            @Query("mine") mine: Boolean = true
        ): Deferred<ChannelsModel>
    }

    interface AccessTokenService {
        @FormUrlEncoded
        @POST(GOOGLE_AUTHORIZATION_SERVER_URL)
        fun getAccessTokenAsync(
            @Field("code") code: String,
            @Field("client_id") client_id: String = ANDROID_CLIENT_ID,
            @Field("redirect_uri") redirect_uri: String = REDIRECT_URI,
            @Field("grant_type") grant_type: String = "authorization_code"
        ): Deferred<AccessTokenModel>
    }


    interface SearchListService {
        @GET("/youtube/v3/search")
        fun getSearchListAsync(
            @Query("key") googleApiKey: String,
            @Query("part") part: String = "snippet",
            @Query("channelId") channelId: String,
            @Query("maxResults") maxResults: Int = 1, // TODO upto 10
            @Query("pageToken") pageToken: String = "",
            @Query("type") type: String
        ): Deferred<SearchListModel>
    }

    /*
    interface VideosService {
        @GET("/youtube/v3/search")
        fun getVideosByChannelIdAsync(
            @Query("key") googleApiKey: String,
            @Query("part") part: String = "snippet",
            @Query("channelId") channelId: String,
            @Query("maxResults") maxResults: Int = 20,
            @Query("pageToken") pageToken: String = ""
        ):
        fun getVideosRequestByChannelId(channelId: String): Request {
            val url = "https://www.googleapis.com/youtube/v3/search?key=$googleApiKey&" +
                    "part=snippet&channelId=$channelId"
            return Request.Builder()
                .url(url)
                .get()
                .build()
        }
    }

     */

    interface PlaylistsService {
        @GET("/youtube/v3/playlists")
        fun getPlaylistsAsync(
            @Query("key") googleApiKey: String,
            @Query("pageToken") pageToken: String = "",
            @Query("part") part: String = "snippet",
            @Query("channelId") channelId: String,
            @Query("maxResults") maxResults: Int = 10
        ): Deferred<PlaylistsModel>
    }

    interface PlaylistItemsService {
        @GET("/youtube/v3/playlistItems")
        fun getPlaylistItemsAsync(
            @Query("key") googleApiKey: String,
            @Query("part") part: String = "snippet,contentDetails",
            @Query("pageToken") pageToken: String = "",
            @Query("playlistId") playlistId: String,
            @Query("maxResults") maxResults: Int = 10
        ): Deferred<PlaylistItemsModel>
    }

    fun accessTokenService(): AccessTokenService =
        createRetrofit().create(AccessTokenService::class.java)

    fun channelsService(): ChannelsService =
        createRetrofit().create(ChannelsService::class.java)

    fun searchListService(): SearchListService =
        createRetrofit().create(SearchListService::class.java)

    fun playlistItemsService(): PlaylistItemsService =
        createRetrofit().create(PlaylistItemsService::class.java)
}