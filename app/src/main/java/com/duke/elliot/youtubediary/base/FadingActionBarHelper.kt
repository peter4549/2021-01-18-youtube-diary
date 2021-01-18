package com.duke.elliot.youtubediary.base

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.manuelpeinado.fadingactionbar.FadingActionBarHelperBase

class FadingActionBarHelper: FadingActionBarHelperBase() {

    private var actionBar: ActionBar? = null

    override fun initActionBar(activity: Activity?) {
        actionBar = (activity as AppCompatActivity).supportActionBar
        super.initActionBar(activity)
    }

    override fun getActionBarHeight(): Int = actionBar?.height ?: 0

    override fun isActionBarNull(): Boolean = actionBar == null

    override fun setActionBarBackgroundDrawable(drawable: Drawable?) {
        actionBar?.setBackgroundDrawable(drawable)
    }

}
