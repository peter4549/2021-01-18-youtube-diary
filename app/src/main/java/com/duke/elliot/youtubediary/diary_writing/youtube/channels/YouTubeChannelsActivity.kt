package com.duke.elliot.youtubediary.diary_writing.youtube.channels

import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.DisplayChannelModel
import com.duke.elliot.youtubediary.database.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ActivityYoutubeChannelsBinding
import com.duke.elliot.youtubediary.diary_writing.youtube.ItemModel
import com.duke.elliot.youtubediary.diary_writing.youtube.YouTubeApi
import com.duke.elliot.youtubediary.diary_writing.youtube.custom_tabs.TooltipActivity
import com.duke.elliot.youtubediary.diary_writing.youtube.custom_tabs.UrlCheckService
import com.duke.elliot.youtubediary.diary_writing.youtube.firestore.FireStoreHelper
import com.duke.elliot.youtubediary.diary_writing.youtube.firestore.UserModel
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.YouTubeVideosActivity
import com.duke.elliot.youtubediary.main.MainApplication
import com.duke.elliot.youtubediary.sign_in.SignInActivity
import kotlinx.android.synthetic.main.item_simple.view.*
import kotlinx.coroutines.*
import timber.log.Timber

class YouTubeChannelsActivity: BaseActivity(), ChannelAdapter.OnItemClickListener, FireStoreHelper.OnDocumentSnapshotListener {

    private lateinit var binding: ActivityYoutubeChannelsBinding
    private lateinit var viewModel: YouTubeChannelsViewModel
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var customTabsClient: CustomTabsClient
    private lateinit var customTabsServiceConnection: CustomTabsServiceConnection
    private var customTabsSession: CustomTabsSession? = null
    private var pageChangeCount = 0

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_youtube_channels)

        val viewModelFactory = YouTubeChannelsViewModelFactory()
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[YouTubeChannelsViewModel::class.java]

        channelAdapter = ChannelAdapter()
        channelAdapter.setOnItemClickListener(this)
        binding.recyclerViewChannel.apply {
            layoutManager = LinearLayoutManager(this@YouTubeChannelsActivity)
            setHasFixedSize(true)
            adapter = channelAdapter
        }

        binding.frameLayoutAddChannel.setOnClickListener {
            bindCustomTabsService()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(UrlCheckService.ACTION_CORRECT_URL)
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.firebaseAuth.currentUser?.let {
            viewModel.registerFireStoreHelper(it.uid, this)
        } ?: run {
            requestSignIn()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_NEW_INTENT) {
            val code = intent.getStringExtra(EXTRA_NAME_AUTHORIZATION_CODE) as String
            getAccessToken(code)
        }
    }

    private fun getAccessToken(code: String) {
        coroutineScope.launch {
            val accessTokenDeferred = YouTubeApi.accessTokenService().getAccessTokenAsync(code)
            try {
                val accessToken = accessTokenDeferred.await().access_token
                getChannels(accessToken)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get access token.")
            }
        }
    }

    private fun getChannels(accessToken: String) {
        coroutineScope.launch {
            val channelsDeferred = YouTubeApi.channelsService().getChannelsAsync("Bearer $accessToken")
            try {
                val channels = channelsDeferred.await().items.map { createChannelModel(it) } as ArrayList

                if (channels.isEmpty())
                    throw Throwable(getString(R.string.channel_not_found))

                val channel = channels[0]

                if (viewModel.channels.notContains(channel)) // TODO, id is equal but other is changed. notify that.
                    viewModel.fireStoreHelper.addChannel(channels[0])
                else
                    showToast(getString(R.string.this_channel_has_already_been_added))
            } catch (t: Throwable) {
                Timber.e(t, "Failed to get channel.")
                showToast(t.message.toString())
            }
        }
    }

    fun submitChannels(channels: List<DisplayChannelModel>) {
        for (channel in channels) {
            if (viewModel.channels.notContains(channel))
                viewModel.channels.add(channel)
        }

        channelAdapter.submitList(channels)
    }

    private fun bindCustomTabsService() {
        customTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName, client: CustomTabsClient
            ) {
                customTabsClient = client
                customTabsClient.warmup(0L)
                customTabsSession = customTabsClient.newSession(object : CustomTabsCallback() {
                    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                        super.onNavigationEvent(navigationEvent, extras)
                        when (navigationEvent) {
                            NAVIGATION_ABORTED -> {  }
                            NAVIGATION_FAILED -> {  }
                            NAVIGATION_FINISHED -> {
                                ++pageChangeCount
                                if (pageChangeCount > 3) {
                                    val intent = Intent(this@YouTubeChannelsActivity, TooltipActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                            NAVIGATION_STARTED -> {  }
                            TAB_HIDDEN -> {
                                pageChangeCount = 0
                            }
                            TAB_SHOWN -> {
                                pageChangeCount = 0
                            }
                        }
                    }
                })

                val builder = CustomTabsIntent.Builder()
                customTabsSession?.let { builder.setSession(it) }
                val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_checkmark_48px)
                val title = "완료." // TODO: change to res.

                builder.setActionButton(icon, title, createPendingIntent(), true)
                builder.setStartAnimations(
                    applicationContext,
                    R.anim.anim_slide_in_right,
                    R.anim.anim_slide_out_left
                )
                builder.setExitAnimations(
                    applicationContext,
                    R.anim.anim_slide_in_left,
                    R.anim.anim_slide_out_right
                )

                val customTabsIntent = builder.build()
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                customTabsIntent.intent.setPackage(CUSTOM_TAB_PACKAGE_NAME)
                customTabsIntent.launchUrl(
                    this@YouTubeChannelsActivity,
                    Uri.parse(YouTubeApi.googleAuthorizationServerUrl)
                )
            }

            override fun onServiceDisconnected(componentName: ComponentName?) {
                Timber.d("Custom tabs service disconnected.")
            }
        }

        if (CustomTabsClient.bindCustomTabsService(
                this,
                CUSTOM_TAB_PACKAGE_NAME,
                customTabsServiceConnection
            )
        )
            Timber.d("Custom tabs service connected.")
        else {
            showToast(getString(R.string.custom_tabs_service_bind_failure_message))
            Timber.e("Failed to connect custom tabs service.")
        }
    }

    private fun createPendingIntent() : PendingIntent {
        stopService(Intent(this, UrlCheckService::class.java))
        val intent = Intent(this, UrlCheckService::class.java)
        return PendingIntent.getService(this, 0, intent, 0)
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UrlCheckService.ACTION_CORRECT_URL) {
                val code = intent.getStringExtra(UrlCheckService.KEY_AUTHORIZATION_CODE) ?: UrlCheckService.INVALID_CODE
                if (code == UrlCheckService.INVALID_CODE) {
                    showToast("유효하지않은 인증 코드") // TODO: change to res.
                    Timber.e("Failed to get authorization code.")
                } else {
                    val selfIntent = Intent(this@YouTubeChannelsActivity, YouTubeChannelsActivity::class.java)
                    selfIntent.action = ACTION_NEW_INTENT
                    selfIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    selfIntent.putExtra(EXTRA_NAME_AUTHORIZATION_CODE, code)
                    startActivity(selfIntent)
                }
            }
        }
    }

    private fun createChannelModel(channelItem: ItemModel): DisplayChannelModel {
        return DisplayChannelModel(
                id = channelItem.id,
                title = channelItem.snippet.title,
                thumbnailUri = channelItem.snippet.thumbnails.medium?.url
                        ?: channelItem.snippet.thumbnails.default.url
        )
    }

    private fun requestSignIn() {
        val message = getString(R.string.request_sign_in_alert_dialog_message)
        showAlertDialog(
            title = getString(R.string.sign_in_with_google),
            message = message,
            neutralButtonText = getString(R.string.cancel),
            neutralButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
                showToast(message)
                finish()
            },
            negativeButtonText = getString(R.string.request_sign_in_alert_dialog_negative_button_text),
            negativeButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
                showToast(message)
                finish()
            },
            positiveButtonText = getString(R.string.sign_in),
            positiveButtonClickListener = { dialogInterface, i ->
                dialogInterface?.dismiss()
                startSignInActivity()
            }
        )
    }

    private fun startSignInActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SIGN_IN_ACTIVITY)
    }

    private fun startYouTubeVideosActivity(channelId: String) {
        val intent = Intent(this, YouTubeVideosActivity::class.java)
        intent.putExtra(EXTRA_NAME_CHANNEL_ID, channelId)
        startActivityForResult(intent, REQUEST_CODE_YOUTUBE_VIDEOS_ACTIVITY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CODE_SIGN_IN_ACTIVITY -> {
                if (resultCode == RESULT_OK) {
                    /** Test code */
                    showToast("success")
                    val uid = MainApplication.getFirebaseAuthInstance().currentUser?.uid
                    uid?.let {
                        viewModel.registerFireStoreHelper(uid, this)
                    }

                    // Load channel info. from user.
                } else
                    showToast("failed")
            }
            REQUEST_CODE_YOUTUBE_VIDEOS_ACTIVITY -> {
                if (resultCode == RESULT_OK) {
                    data?.getParcelableExtra<DisplayVideoModel>(
                        YouTubeVideosActivity.EXTRA_NAME_DISPLAY_VIDEO_MODEL
                    )?.let {
                        val intent = Intent()
                        intent.putExtra(EXTRA_NAME_DISPLAY_VIDEO_MODEL, it)
                        setResult(RESULT_OK, intent)
                        finish()
                    } ?: run {
                        Timber.e(NullPointerException("Video not found."))
                        showToast(getString(R.string.video_not_found))
                    }
                }
            }
        }
    }

    override fun onClick(channelId: String) {
        startYouTubeVideosActivity(channelId)
    }

    override fun onUserDocumentSnapshot(user: UserModel) {
        val channels = user.youtubeChannels
        submitChannels(channels)
    }

    companion object {
        private const val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome"
        private const val ACTION_NEW_INTENT = "com.duke.elliot.youtubediary.diary_writing.youtube.you_tube_channels_activity" +
                ".action_new_intent"
        private const val EXTRA_NAME_AUTHORIZATION_CODE = "com.duke.elliot.youtubediary.diary_writing.youtube.you_tube_channels_activity" +
                ".extra_name_authorization_code"

        private const val REQUEST_CODE_SIGN_IN_ACTIVITY = 1746
        private const val REQUEST_CODE_YOUTUBE_VIDEOS_ACTIVITY = 1747

        const val EXTRA_NAME_CHANNEL_ID = "com.duke.elliot.youtubediary.diary_writing.youtube.channels" +
                ".youtube_channels_activity.extra_name_channel_id"
        const val EXTRA_NAME_DISPLAY_VIDEO_MODEL = "com.duke.elliot.youtubediary.diary_writing.youtube.channels" +
                ".youtube_channels_activity.extra_name_display_video_model"
    }

    private fun MutableList<DisplayChannelModel>.notContains(element: DisplayChannelModel) = !contains(element)
}