package com.duke.elliot.youtubediary.diary_writing.youtube.custom_tabs

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duke.elliot.youtubediary.R
import timber.log.Timber

class UrlCheckService: Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.dataString ?: ""
        if (url.startsWith("https://accounts.google.com/o/oauth2/approval/v2")) {
            val code = intent?.data?.getQueryParameter("approvalCode") ?: INVALID_CODE
            sendBroadcast(code)
        } else
            Toast.makeText(applicationContext, getString(R.string.click_on_login_page_toast), Toast.LENGTH_LONG).show()


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendBroadcast(code: String) {
        val intent = Intent(ACTION_CORRECT_URL)
        intent.putExtra(KEY_AUTHORIZATION_CODE, code)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Timber.d("Authorization code sent.")
        stopSelf()
    }

    companion object {
        const val ACTION_CORRECT_URL = "com.duke.elliot.youtubediary.diary_writing.youtube.url_check_service.action_correct_url"
        const val INVALID_CODE = "com.duke.elliot.youtubediary.diary_writing.youtube.url_check_service.invalid_code"
        const val KEY_AUTHORIZATION_CODE = "com.duke.elliot.youtubediary.diary_writing.youtube.url_check_service.key_authorization_code"
    }
}