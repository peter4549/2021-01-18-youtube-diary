package com.duke.elliot.youtubediary.diary_writing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.widget.PopupMenu
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
import com.duke.elliot.youtubediary.diary_writing.youtube.player.YouTubePlayerActivity
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.YouTubeVideosActivity
import com.duke.elliot.youtubediary.fluid_keyboard_resize.FluidContentResize
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment
import com.duke.elliot.youtubediary.folder.FolderSearchBarListDialogFragment
import com.duke.elliot.youtubediary.util.SearchBarListDialogFragment
import com.duke.elliot.youtubediary.util.toDateFormat
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.github.ksoichiro.android.observablescrollview.ScrollUtils
import kotlinx.android.synthetic.main.activity_diary_writing.*
import kotlinx.coroutines.*
import timber.log.Timber

class DiaryWritingActivity: BaseActivity(), ObservableScrollViewCallbacks,
    SearchBarListDialogFragment.FragmentContainer, ViewPagerAdapter.OnItemClickListener {

    private lateinit var binding: ActivityDiaryWritingBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var viewModel: DiaryWritingViewModel

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private var mode = MODE_CREATE
    private var parallaxImageHeight = 0
    private var toolbarHeight = 0

    private lateinit var folderSearchBarListDialog: FolderSearchBarListDialogFragment

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

        viewPagerAdapter.setOnItemClickListener(this)

        binding.viewPager.adapter = viewPagerAdapter
        binding.wormDotsIndicator.setViewPager2(binding.viewPager)
        binding.imageViewPagerOptions.setOnClickListener {
            showPopupMenu(it)
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
    }

    private fun initUI() {
        binding.textDate.text = viewModel.updatedAt.toDateFormat(getString(R.string.date_format))
        binding.textTime.text = viewModel.updatedAt.toDateFormat(getString(R.string.time_format))

        binding.editTextContent.setText(viewModel.content)

        binding.textFolder.text = viewModel.folder?.name

        binding.imageFolder.setOnClickListener {
            showFolderSearchBarListDialog()
        }
    }

    /** TODO: Check well here. */
    private fun modified(): Boolean {
        if (mode == MODE_CREATE)
            return false

        val content = binding.editTextContent.text.toString()

        if (content != viewModel.content)
            return true

        if (viewModel.originalDiary?.folder != viewModel.folder)
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
        val color = Color.RED
        binding.toolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, color))

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
        val editFolderDialog = EditFolderDialogFragment(viewModel.folderDao, mode, folder)
        editFolderDialog.let {
            it.show(supportFragmentManager, it.tag)
        }
    }

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
                }
            }

            true
        }

        popupMenu.show()
    }

    /** FolderSearchBarListDialogFragment */
    override fun <Folder> onRequestListener(): SearchBarListDialogFragment.OnClickListener<Folder> =
        object: SearchBarListDialogFragment.OnClickListener<Folder> {
            override fun onListItemClick(listItem: Folder) {
                if (listItem is com.duke.elliot.youtubediary.database.Folder) {
                    if (viewModel.folder != listItem) {
                        viewModel.folder = listItem
                        binding.textFolder.text = listItem.name
                    }
                }

                // TODO: save시 데이터 베이스 업로드 할 것. 아직 확정단계가 아님 여기는.
            }

            override fun onAddClick() {
                showEditFolderDialog(EditFolderDialogFragment.MODE_ADD, null)
            }

            override fun moreOptionsClick(itemId: Int, listItem: Folder) {
                if (listItem is com.duke.elliot.youtubediary.database.Folder) {
                    when (itemId) {
                        R.id.item_edit -> showEditFolderDialog(EditFolderDialogFragment.MODE_EDIT, listItem)
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