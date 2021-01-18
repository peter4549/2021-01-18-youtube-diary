package com.duke.elliot.youtubediary.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import timber.log.Timber

class ClickableViewPager : ViewPager {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var singleTapUpListener: () -> Unit

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    fun setSingleTapUpListener(singleTapUpListener: () -> Unit) {
        this.singleTapUpListener = singleTapUpListener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (this.childCount > 1)
            this.requestDisallowInterceptTouchEvent(true)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setup() {
        gestureDetector = GestureDetector(context, SingleTapListener())
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.onTouchEvent(ev)
    }

    private inner class SingleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            if (::singleTapUpListener.isInitialized)
                singleTapUpListener.invoke()
            else
                Timber.e("singleTapUpListener not initialized.")
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (kotlin.math.abs(distanceY) > kotlin.math.abs(distanceX))
                requestDisallowInterceptTouchEvent(false)

            return true
        }
    }
}