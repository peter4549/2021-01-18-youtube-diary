package com.duke.elliot.youtubediary.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.text.Html
import android.widget.TextView
import androidx.annotation.ColorInt
import com.duke.elliot.youtubediary.R
import java.text.SimpleDateFormat
import java.util.*

fun Number?.isNull() = this == null
fun Number?.isNotNull() = this != null

fun Int.isZero() = this == 0

fun Int.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Float.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Long.toDateFormat(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this)

fun Int.toHexColor() = String.format("#%06X", 0xFFFFFF and this)

fun setTextAndChangeSearchWordColor(
    textView: TextView, text: String,
    searchWord: String, @ColorInt color: Int?
) {
    if (color == null) {
        textView.text = text
        return
    }

    if (text.isBlank()) {
        textView.text = text
        return
    }

    val hexColor = color.toHexColor()
    val htmlText = text.replaceFirst(
        searchWord,
        "<font color='$hexColor'>$searchWord</font>"
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
        textView.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
    else
        @Suppress("DEPRECATION")
        textView.text = Html.fromHtml(htmlText)
}

fun shareApplication(context: Context) {
    val intent = Intent(Intent.ACTION_SEND)
    val text = "https://play.google.com/store/apps/details?id=${context.packageName}"

    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)

    val chooser = Intent.createChooser(intent, context.getString(R.string.share_the_app))
    context.startActivity(chooser)
}

fun getVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        return "Version info not found."
    }
}

fun goToPlayStore(context: Context) {
    try {
        context.startActivity(
            Intent (
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${context.packageName}"))
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent (
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
            )
        )
    }
}