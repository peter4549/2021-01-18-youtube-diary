package com.duke.elliot.youtubediary.diary_writing.youtube.custom_tabs

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.databinding.ActivityTooltipBinding
import com.duke.elliot.youtubediary.main.MainApplication


class TooltipActivity: Activity() {

    private lateinit var binding: ActivityTooltipBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tooltip)
        window.setDimAmount(0F)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.root.setOnClickListener {
            finish()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val view = window.decorView
        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        layoutParams.gravity = Gravity.END or Gravity.TOP
        layoutParams.x = resources.getDimensionPixelSize(R.dimen.custom_tabs_tooltip_margin_end)
        layoutParams.y = resources.getDimensionPixelSize(R.dimen.custom_tabs_tooltip_margin_top)
        windowManager.updateViewLayout(view, layoutParams)
    }
}