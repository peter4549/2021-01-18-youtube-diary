package com.duke.elliot.youtubediary.diary_writing

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.DEFAULT_FOLDER_ID
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ActivityDiaryWritingBinding
import com.duke.elliot.youtubediary.diary_writing.youtube.channels.YouTubeChannelsActivity
import com.duke.elliot.youtubediary.diary_writing.youtube.player.YouTubePlayerActivity
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.YouTubeVideosActivity
import com.duke.elliot.youtubediary.fluid_keyboard_resize.FluidContentResize
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment
import com.duke.elliot.youtubediary.folder.FolderSearchBarListDialogFragment
import com.duke.elliot.youtubediary.main.MainApplication
import com.duke.elliot.youtubediary.util.*
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.github.ksoichiro.android.observablescrollview.ScrollUtils
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.*
import timber.log.Timber

class DiaryWritingActivity: BaseActivity(), ObservableScrollViewCallbacks,
    SearchBarListDialogFragment.FragmentContainer, ViewPagerAdapter.OnItemClickListener,
    EditFolderDialogFragment.FragmentContainer {

    private lateinit var binding: ActivityDiaryWritingBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var viewModel: DiaryWritingViewModel

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private var mode = MODE_CREATE
    private var parallaxImageHeight = 0
    private var toolbarHeight = 0

    private lateinit var folderSearchBarListDialog: FolderSearchBarListDialogFragment

    private var recognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private var isRecognizing = false
    private var errorNoMatchCount = 0
    private val recognitionListener: RecognitionListener

    init {
        recognitionListener = object: RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            // Full speech recognition results are not obtained.
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> Timber.e("SpeechRecognizer.ERROR_AUDIO")
                    SpeechRecognizer.ERROR_CLIENT -> Timber.e("SpeechRecognizer.ERROR_CLIENT")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Timber.e("SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS")
                    SpeechRecognizer.ERROR_NETWORK -> Timber.e("SpeechRecognizer.ERROR_NETWORK")
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Timber.e("SpeechRecognizer.ERROR_NETWORK_TIMEOUT")
                    SpeechRecognizer.ERROR_NO_MATCH -> { Timber.e("SpeechRecognizer.ERROR_NO_MATCH") }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Timber.e("SpeechRecognizer.ERROR_RECOGNIZER_BUSY")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> { Timber.e("SpeechRecognizer.ERROR_SPEECH_TIMEOUT") }
                }

                when (error) {
                    SpeechRecognizer.ERROR_CLIENT -> { /** Continue. */ }
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        if (errorNoMatchCount > 1)
                            stopRecognition()
                        else {
                            recognizer?.stopListening()
                            recognizer?.startListening(recognizerIntent)
                            ++errorNoMatchCount
                        }
                    }
                    else -> stopRecognition()
                }
            }

            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.let { resultsRecognition ->
                        if (resultsRecognition.isNullOrEmpty())
                            return@let

                        binding.editTextContent.append("${resultsRecognition[0]} ")
                        recognizer?.stopListening()
                        recognizer?.startListening(recognizerIntent)
                        errorNoMatchCount = 0
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary_writing)
        binding.observableScrollView.setScrollViewCallbacks(this)

        mode = intent.getIntExtra(EXTRA_NAME_MODE, MODE_CREATE)
        val originalDiary = intent.getParcelableExtra<Diary>(EXTRA_NAME_DIARY)  // Get existing diary.

        val viewModelFactory = DiaryWritingViewModelFactory(application, originalDiary)
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[DiaryWritingViewModel::class.java]

        viewPagerAdapter = ViewPagerAdapter().apply {
            if (viewModel.youtubeVideos.isNotEmpty())
                addAll(viewModel.youtubeVideos)
        }

        viewPagerAdapter.setOnItemClickListener(this)

        binding.toolbar.title = ""
        binding.viewPager.adapter = viewPagerAdapter
        binding.wormDotsIndicator.setViewPager2(binding.viewPager)
        binding.imageViewPagerOptions.setOnClickListener {
            showPopupMenu(it)
        }

        binding.imageSpeechRecognition.setOnClickListener {
            if (isRecognizing)
                return@setOnClickListener

            createPermissionListener()
        }

        binding.frameLayoutRecognitionProgressView.setOnClickListener {
            stopRecognition()
        }

        binding.imageSave.setOnClickListener {
            save()
        }

        setSupportActionBar(binding.toolbar)
        setOptionsMenu(R.menu.menu_diary_writing_activity)
        setOnOptionsItemSelectedListeners(
            R.id.item_addYouTubeVideo to {
                val intent = Intent(this, YouTubeChannelsActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_YOUTUBE_VIDEO)
            }
        )

        parallaxImageHeight = resources.getDimensionPixelSize(R.dimen.parallax_image_height)
        toolbarHeight = actionBarHeight()

        FluidContentResize.listen(this)
        initUI()
        setLinearLayoutFolderHeight()

        setDisplayHomeAsUpEnabled(binding.toolbar)
        setOnHomePressedCallback { onBackPressed() }

        /** Folder */
        viewModel.folder.observe(this, { folder ->
            folder?.let {
                if (!binding.linearLayoutFolder.isVisible)
                    binding.linearLayoutFolder.visibility = View.VISIBLE
                binding.textFolder.text = folder.name
            } ?: run {
                binding.linearLayoutFolder.visibility = View.INVISIBLE
            }
        })
    }

    override fun onBackPressed() {
        if (isRecognizing) {
            stopRecognition()
            return
        }

        if (edited())
            showSaveConfirmationDialog()
        else {
            if (mode == MODE_CREATE) {
                val content = binding.editTextContent.text.toString()

                if (content.isBlank() && viewModel.noFolder() && viewModel.noYouTubeVideos())
                    finish()
                else
                    showSaveConfirmationDialog()
            } else if (mode == MODE_EDIT) {
                showToast(getString(R.string.nothing_has_changed))
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun showSaveConfirmationDialog() {
        showMaterialAlertDialog(
            title = getString(R.string.save_confirmation_dialog_title),
            message = getString(R.string.save_confirmation_dialog_message),
            neutralButtonText = getString(R.string.cancel),
            neutralButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
            },
            negativeButtonText = getString(R.string.do_not_save),
            negativeButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
                finish()
            },
            positiveButtonText = getString(R.string.save),
            positiveButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()

                if (mode == MODE_CREATE)
                    viewModel.saveDiary(createDiary())
                else if (mode == MODE_EDIT)
                    updatedDiary()?.let { viewModel.updateDiary(it) }

                finish()
            }
        )
    }

    private fun initUI() {
        binding.textDate.text = viewModel.updatedAt.toDateFormat(getString(R.string.date_format))
        binding.textTime.text = viewModel.updatedAt.toDateFormat(getString(R.string.time_format))

        binding.editTextContent.setText(viewModel.originalContent)

        binding.imageFolder.setOnClickListener {
            showFolderSearchBarListDialog()
        }
    }

    /** Content, Folder, YouTubeVideos. */
    private fun edited(): Boolean {
        if (mode == MODE_CREATE)
            return false

        val content = binding.editTextContent.text.toString()

        if (content != viewModel.originalContent)
            return true

        if (viewModel.originalFolderId != (viewModel.folder.value?.id ?: DEFAULT_FOLDER_ID))
            return true

        if (viewModel.originalDiary?.youtubeVideos?.toList() != viewModel.youtubeVideos)
            return true

        return false
    }

    private fun save() {
        if (edited()) {
            updatedDiary()?.let { viewModel.updateDiary(it) } ?: run {
                showToast(getString(R.string.diary_not_found))
            }

            finish()
        } else {
            if (mode == MODE_CREATE) {
                val content = binding.editTextContent.text.toString()

                if (content.isBlank() && viewModel.noFolder() && viewModel.noYouTubeVideos())
                    finish()
                else {
                    viewModel.saveDiary(createDiary())
                    finish()
                }
            } else if (mode == MODE_EDIT) {
                showToast(getString(R.string.nothing_has_changed))
                finish()
            }
        }
    }

    private fun createDiary(): Diary = Diary(
        updatedAt = viewModel.updatedAt,
        content = binding.editTextContent.text.toString(),
        folderId = viewModel.folder.value?.id ?: DEFAULT_FOLDER_ID,
        youtubeVideos = viewModel.youtubeVideos.toTypedArray()
    )

    private fun updatedDiary(): Diary? {
        viewModel.originalDiary?.updatedAt = System.currentTimeMillis()
        viewModel.originalDiary?.content = binding.editTextContent.text.toString()
        viewModel.originalDiary?.folderId = viewModel.folder.value?.id ?: DEFAULT_FOLDER_ID
        viewModel.originalDiary?.youtubeVideos = viewModel.youtubeVideos.toTypedArray()

        return viewModel.originalDiary
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CODE_ADD_YOUTUBE_VIDEO -> {
                if (resultCode == RESULT_OK) {
                    val displayVideoModel = data?.getParcelableExtra<DisplayVideoModel>(
                        YouTubeChannelsActivity.EXTRA_NAME_DISPLAY_VIDEO_MODEL
                    )

                    displayVideoModel?.let {
                        if (binding.relativeLayoutViewPager.isNotVisible())
                            binding.relativeLayoutViewPager.visibility = View.VISIBLE

                        if (binding.viewAnchor.isVisible)
                            binding.viewAnchor.visibility = View.GONE

                        // Duplicate check.
                        if (viewModel.youtubeVideos.notContain(it)) {
                            viewModel.addVideo(video = it)
                            viewPagerAdapter.add(item = it)
                            coroutineScope.launch {
                                delay(100L)
                                binding.viewPager.setCurrentItem(0, false)
                                binding.wormDotsIndicator.setViewPager2(binding.viewPager)
                            }
                        } else
                            showToast(getString(R.string.this_video_has_already_been_added))
                    }
                }
            }
        }
    }

    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
        val alpha = 1F.coerceAtMost(scrollY / (parallaxImageHeight - toolbarHeight).toFloat())
        binding.toolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, MainApplication.primaryThemeColor))

        if (binding.relativeLayoutViewPager.isVisible)
            binding.relativeLayoutViewPager.translationY = scrollY / 2F
    }

    override fun onDownMotionEvent() {

    }

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {

    }

    private fun showFolderSearchBarListDialog() {
        folderSearchBarListDialog = FolderSearchBarListDialogFragment()

        if (::folderSearchBarListDialog.isInitialized)
            folderSearchBarListDialog.let {
                it.moreOptionsMenuRes = R.menu.menu_folder_search_bar_list_dialog_fragment
                it.show(supportFragmentManager, it.tag)
            }
    }

    private fun showEditFolderDialog(mode: Int, folder: Folder?) {
        val editFolderDialog = EditFolderDialogFragment().apply {
            setMode(mode)
            folder?.let { setFolder(it) }
        }

        editFolderDialog.let {
            it.show(supportFragmentManager, it.tag)
        }
    }

    // Delete video.
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_view_pager)
        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.item_delete -> {
                    val video = viewPagerAdapter.getItem(binding.viewPager.currentItem)
                    viewModel.youtubeVideos.remove(video)
                    viewPagerAdapter.remove(video)
                    binding.wormDotsIndicator.setViewPager2(binding.viewPager)

                    if (viewModel.youtubeVideos.isEmpty()) {
                        binding.relativeLayoutViewPager.visibility = View.GONE
                        binding.viewAnchor.visibility = View.VISIBLE
                    }
                }
            }

            true
        }

        popupMenu.show()
    }

    override fun onRequestOnClickListener(): EditFolderDialogFragment.OnClickListener =
        object: EditFolderDialogFragment.OnClickListener {
            override fun onPositiveButtonClick(folder: Folder) {
                if (folder.id == viewModel.folder.value?.id) {
                    // Update UI.
                    viewModel.folder.value = folder
                }
            }
        }

    /** FolderSearchBarListDialogFragment */
    override fun <Folder> onRequestListener(): SearchBarListDialogFragment.OnClickListener<Folder> =
        object: SearchBarListDialogFragment.OnClickListener<Folder> {
            override fun onListItemClick(listItem: Folder) {
                if (listItem is com.duke.elliot.youtubediary.database.Folder) {
                    if (viewModel.folder.value?.id != listItem.id) {
                        viewModel.folder.value = listItem
                    }
                }
            }

            override fun onAddClick() {
                showEditFolderDialog(EditFolderDialogFragment.MODE_ADD, null)
            }

            override fun moreOptionsClick(itemId: Int, listItem: Folder) {
                if (listItem is com.duke.elliot.youtubediary.database.Folder) {
                    when (itemId) {
                        R.id.item_edit -> showEditFolderDialog(
                            EditFolderDialogFragment.MODE_EDIT,
                            listItem
                        )
                        R.id.item_delete -> viewModel.deleteFolder(listItem)
                    }
                }
            }
        }

    override fun onClick(displayVideo: DisplayVideoModel) {
        val videoId = displayVideo.id
        val intent = Intent(this, YouTubePlayerActivity::class.java)
        intent.putExtra(YouTubeVideosActivity.EXTRA_NAME_VIDEO_ID, videoId)
        startActivity(intent)
    }

    /** Speech Recognizer */
    private fun startRecognition() {
        if (recognizer.isNull())
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, VALUE_LANGUAGE)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, VALUE_MAX_RESULTS)
        // recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // Unused.

        recognizer?.setRecognitionListener(recognitionListener)

        isRecognizing = true
        errorNoMatchCount = 0
        binding.frameLayoutRecognitionProgressView.fadeIn(200)
        binding.recognitionProgressView.setColors(
            intArrayOf(
                ContextCompat.getColor(this, R.color.speech_recognition_view_00),
                ContextCompat.getColor(this, R.color.speech_recognition_view_01),
                ContextCompat.getColor(this, R.color.speech_recognition_view_02),
                ContextCompat.getColor(this, R.color.speech_recognition_view_03),
                ContextCompat.getColor(this, R.color.speech_recognition_view_04)
            )
        )

        binding.recognitionProgressView.setSpeechRecognizer(recognizer)
        binding.recognitionProgressView.setRecognitionListener(recognitionListener)
        binding.recognitionProgressView.play()
        recognizer?.startListening(recognizerIntent)
    }

    private fun stopRecognition() {
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        binding.recognitionProgressView.stop()
        binding.frameLayoutRecognitionProgressView.fadeOut(200)
        isRecognizing = false
    }

    private fun createPermissionListener() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                startRecognition()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                showSnackbarOnDenied()
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }
        }

        Dexter.withContext(this)
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(permissionListener)
            .check()
    }

    @Suppress("SpellCheckingInspection")
    private fun showSnackbarOnDenied() {
        val snackbar = Snackbar
            .make(
                binding.root,
                getString(R.string.snackbar_on_denied_message_record_audio),
                5000
            )
            .setAction(getString(R.string.settings)) {
                openApplicationSettings()
            }
            .setActionTextColor(ContextCompat.getColor(this, R.color.text_accent_dark))

        snackbar.show()

        val snackbarActionTextView =
            snackbar.view.findViewById<View>(com.google.android.material.R.id.snackbar_action) as TextView
        val font = ResourcesCompat.getFont(applicationContext, R.font.font_family_well_today_medium)
        snackbarActionTextView.setTypeface(font, Typeface.BOLD)
        snackbarActionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)

        val snackbarTextView =
            snackbar.view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        snackbarTextView.maxLines = 5
        snackbarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
    }

    private fun openApplicationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun setLinearLayoutFolderHeight() {
        binding.constraintLayoutContent.post(Runnable {
            run {
                binding.viewAnchor.post(Runnable {
                    run {
                        val height = binding.viewAnchor.height +
                                binding.constraintLayoutContent.height -
                                resources.getDimension(R.dimen.spacing_smallest).toInt()
                        val linearLayoutFolderHeight = screenHeight() - height
                        val params = binding.linearLayoutFolder.layoutParams
                        params.height = linearLayoutFolderHeight
                        binding.linearLayoutFolder.layoutParams = params

                        if (viewModel.youtubeVideos.isNotEmpty()) {
                            if (binding.relativeLayoutViewPager.isNotVisible())
                                binding.relativeLayoutViewPager.visibility = View.VISIBLE

                            if (binding.viewAnchor.isVisible)
                                binding.viewAnchor.visibility = View.GONE
                        }
                    }
                })
            }
        })
    }

    private fun screenHeight(): Int {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val windowInsets = metrics.windowInsets
            val insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()
                        or WindowInsets.Type.displayCutout()
            )

            val insetsWidth: Int = insets.right + insets.left
            val insetsHeight: Int = insets.top + insets.bottom

            val bounds = metrics.bounds
            val size = Size(
                bounds.width() - insetsWidth,
                bounds.height() - insetsHeight
            )

            return size.height
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(displayMetrics)

            return displayMetrics.heightPixels
        }
    }

    private fun SpeechRecognizer?.isNull() = this == null

    companion object {
        private const val REQUEST_CODE_ADD_YOUTUBE_VIDEO = 2038

        const val MODE_CREATE = 0
        const val MODE_EDIT = 1

        const val EXTRA_NAME_DIARY = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_name_diary"
        const val EXTRA_NAME_MODE = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_name_mode"

        private const val VALUE_LANGUAGE = "ko-KR"
        private const val VALUE_MAX_RESULTS = 5
    }

    private fun RelativeLayout.isNotVisible() = !isVisible
    private fun MutableList<DisplayVideoModel>.notContain(element: DisplayVideoModel) = !contains(
        element
    )
}