package com.duke.elliot.youtubediary.util

import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.*

fun Number?.isNull() = this == null
fun Number?.isNotNull() = this != null

fun Int.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Float.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Long.toDateFormat(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this)

fun Int.toHexColor() = String.format("#%06X", 0xFFFFFF and this)