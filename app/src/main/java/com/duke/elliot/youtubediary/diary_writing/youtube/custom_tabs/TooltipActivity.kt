package com.duke.elliot.youtubediary.diary_writing.youtube.custom_tabs

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.databinding.ActivityTooltipBinding


class TooltipActivity: Activity() {

    private lateinit var binding: ActivityTooltipBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tooltip)
        window.setDimAmount(0F)

        binding.root.setOnClickListener {
            finish()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val view = window.decorView
        val lp = view.layoutParams as WindowManager.LayoutParams
        lp.gravity = Gravity.END or Gravity.TOP
        lp.x = 10
        lp.y = 128
        windowManager.updateViewLayout(view, lp)
    }
}