package com.duke.elliot.youtubediary.diary_writing

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseActivity
import com.duke.elliot.youtubediary.base.FadingActionBarHelper
import com.duke.elliot.youtubediary.diary_writing.youtube.channels.YouTubeChannelsActivity
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.DisplayVideoModel
import kotlinx.android.synthetic.main.header_layout.*

class DiaryWritingActivity: BaseActivity() {

    private var mode = CREATE
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    // TODO move to viewModel..
    private val displayVideoModels = mutableListOf<DisplayVideoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO check this..
        val fadingActionBarHelper = FadingActionBarHelper()
            .actionBarBackground<FadingActionBarHelper>(R.drawable.ic_sharp_edit_24)
            .headerLayout<FadingActionBarHelper>(R.layout.header_layout)
            .contentLayout<FadingActionBarHelper>(R.layout.activity_diary_writing)
        setContentView(fadingActionBarHelper.createView(this))
        fadingActionBarHelper.initActionBar(this)

        viewPagerAdapter = ViewPagerAdapter(this)
        // TODO if edit mode, add item here.
        viewPager.removeAllViews()
        viewPager.adapter = viewPagerAdapter

        setOptionsMenu(R.menu.menu_diary_writing_activity)
        setOnOptionsItemSelectedListeners(
            R.id.item_addYouTubeVideo to {
                val intent = Intent(this, YouTubeChannelsActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_YOUTUBE_VIDEO)
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CODE_ADD_YOUTUBE_VIDEO -> {
                if (resultCode == RESULT_OK) {
                    val displayVideoModel = data?.getParcelableExtra<DisplayVideoModel>(YouTubeChannelsActivity.EXTRA_NAME_DISPLAY_VIDEO_MODEL)
                    displayVideoModel?.let {
                        if (relativeLayout_viewPager.isNotVisible())
                            relativeLayout_viewPager.visibility = View.VISIBLE

                        displayVideoModels.add(it)
                        viewPager.removeAllViews()
                        viewPagerAdapter.setItems(displayVideoModels)
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
}