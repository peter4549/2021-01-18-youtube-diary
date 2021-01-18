package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.content.Context
import com.duke.elliot.youtubediary.R
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TimeAgoFormatConverter(private val context: Context) {
    private val simpleDateFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        Locale.getDefault()
    )

    private fun getString(resId: Int) = context.getString(resId)

    init {
        val calendar = Calendar.getInstance()
        simpleDateFormat.timeZone = calendar.timeZone
    }

    fun covertToTimeAgoFormat(publishedAt: String): String? {
        var timeAgo: String? = null
        @Suppress("UNUSED_VARIABLE")
        val prefix = ""
        val suffix = getString(R.string.ago)

        try {
            val diff = System.currentTimeMillis() - (simpleDateFormat.parse(publishedAt)?.time ?: 0L)
            val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours: Long = TimeUnit.MILLISECONDS.toHours(diff)
            val days: Long = TimeUnit.MILLISECONDS.toDays(diff)
            when {
                seconds < 60 -> timeAgo = "$seconds${getString(R.string.seconds)} $suffix"
                minutes < 60 -> timeAgo = "$minutes${getString(R.string.minutes)} $suffix"
                hours < 24 -> timeAgo = "$hours${getString(R.string.hours)} $suffix"
                days >= 7 -> {
                    timeAgo = when {
                        days > 360 -> (days / 360).toString() + "${getString(R.string.years)} " + suffix
                        days > 30 -> (days / 30).toString() + "${getString(R.string.months)} " + suffix
                        else -> (days / 7).toString() + "${getString(R.string.weeks)} " + suffix
                    }
                }
                days < 7 ->
                    timeAgo = "$days${getString(R.string.days)} $suffix"
            }
        } catch (e: ParseException) {
            Timber.e(e, "Conversion to time ago format failed.")
        }

        return timeAgo
    }
}