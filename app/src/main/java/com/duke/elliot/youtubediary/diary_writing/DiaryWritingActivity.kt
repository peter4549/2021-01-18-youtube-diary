package com.duke.elliot.youtubediary.diary_writing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ActivityDiaryWritingBinding
import com.duke.elliot.youtubediary.diary_writing.youtube.channels.YouTubeChannelsActivity
import com.duke.elliot.youtubediary.fluid_keyboard_resize.FluidContentResize
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment
import com.duke.elliot.youtubediary.folder.FolderSearchBarListDialogFragment
import com.duke.elliot.youtubediary.util.toDateFormat
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.github.ksoichiro.android.observablescrollview.ScrollUtils

class DiaryWritingActivity: BaseActivity(), ObservableScrollViewCallbacks {

    private lateinit var binding: ActivityDiaryWritingBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var viewModel: DiaryWritingViewModel

    private var mode = MODE_CREATE
    private var parallaxImageHeight = 0
    private var toolbarHeight = 0

    private val displayVideoModels = mutableListOf<DisplayVideoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary_writing)
        binding.observableScrollView.setScrollViewCallbacks(this)

        mode = intent.getIntExtra(EXTRA_MODE, MODE_CREATE)
        val originalDiary = intent.getParcelableExtra<Diary>(EXTRA_NAME_DIARY)

        val viewModelFactory = DiaryWritingViewModelFactory(application, originalDiary)
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[DiaryWritingViewModel::class.java]

        viewPagerAdapter = ViewPagerAdapter().apply {
            if (viewModel.youtubeVideos.isNotEmpty()) {
                if (binding.relativeLayoutViewPager.isNotVisible())
                    binding.relativeLayoutViewPager.visibility = View.VISIBLE

                if (binding.viewAnchor.isVisible)
                    binding.viewAnchor.visibility = View.GONE

                addAll(viewModel.youtubeVideos)
            }
        }
        binding.viewPager.adapter = viewPagerAdapter

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
    }

    private fun initUI() {
        binding.textDate.text = viewModel.updatedAt.toDateFormat(getString(R.string.date_format))
        binding.textTime.text = viewModel.updatedAt.toDateFormat(getString(R.string.time_format))

        binding.editTextContent.setText(viewModel.content)

        binding.textFolder.text = viewModel.folder

        binding.imageFolder.setOnClickListener {
            showFolderSearchBarListDialog(it.context)
        }
    }

    /** TODO: Check well here. */
    private fun modified(): Boolean {
        if (mode == MODE_CREATE)
            return false

        val content = binding.editTextContent.text.toString()
        val folder = binding.textFolder.text.toString()

        if (content != viewModel.content)
            return true

        if (folder != viewModel.folder)
            return true

        return false
    }

    private fun save() {

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
                            viewModel.addVideo(it)
                            viewPagerAdapter.add(item = it)
                        }
                    }
                }
            }
        }
    }

    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
        val alpha = 1F.coerceAtMost(scrollY / (parallaxImageHeight - toolbarHeight).toFloat())
        val color = Color.RED
        binding.toolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, color))

        if (binding.relativeLayoutViewPager.isVisible)
            binding.relativeLayoutViewPager.translationY = scrollY / 2F
    }

    override fun onDownMotionEvent() {

    }

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {

    }

    private fun showFolderSearchBarListDialog(context: Context) {
        val folderSearchBarListDialog = FolderSearchBarListDialogFragment(context).apply {
            setImageAddCallback {
                showEditFolderDialog(EditFolderDialogFragment.MODE_ADD, null)
            }
        }
        folderSearchBarListDialog.let {
            it.show(supportFragmentManager, it.tag)
        }
    }

    private fun showEditFolderDialog(mode: Int, folder: Folder?) {
        val editFolderDialog = EditFolderDialogFragment(viewModel.folderDao, mode, folder)
        editFolderDialog.let {
            it.show(supportFragmentManager, it.tag)
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD_YOUTUBE_VIDEO = 2038

        const val MODE_CREATE = 0
        const val MODE_EDIT = 1

        const val EXTRA_NAME_DIARY_WRITING_ACTIVITY = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_name_diary_writing_activity"
        const val EXTRA_NAME_DIARY = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_name_diary"
        const val EXTRA_MODE = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_mode"
    }

    private fun RelativeLayout.isNotVisible() = !isVisible
    private fun MutableList<DisplayVideoModel>.notContain(element: DisplayVideoModel) = !contains(element)
}