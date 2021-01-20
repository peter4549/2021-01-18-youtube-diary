package com.duke.elliot.youtubediary.diary_writing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.database.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ActivityDiaryWritingBinding
import com.duke.elliot.youtubediary.diary_writing.youtube.channels.YouTubeChannelsActivity
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.github.ksoichiro.android.observablescrollview.ScrollUtils

class DiaryWritingActivity: BaseActivity(), ObservableScrollViewCallbacks {

    private lateinit var binding: ActivityDiaryWritingBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private var mode = CREATE
    private var parallaxImageHeight = 0

    // TODO move to viewModel..
    private val displayVideoModels = mutableListOf<DisplayVideoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary_writing)
        binding.observableScrollView.setScrollViewCallbacks(this)

        viewPagerAdapter = ViewPagerAdapter()
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

                        viewPagerAdapter.addItem(it)
                    }
                }
            }
        }
    }

    private fun RelativeLayout.isNotVisible() = !isVisible

    companion object {
        private const val REQUEST_CODE_ADD_YOUTUBE_VIDEO = 2038

        const val CREATE = 0
        const val EDIT = 1

        const val EXTRA_NAME_DIARY_WRITING_ACTIVITY = "com.duke.elliot.youtubediary.diary_writing.diary_writing_activity." +
                "extra_name_diary_writing_activity"
    }

    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
        val alpha = 1F.coerceAtMost(scrollY / parallaxImageHeight.toFloat())
        val color = Color.RED
        binding.toolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, color))

    }

    override fun onDownMotionEvent() {

    }

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {

    }
}