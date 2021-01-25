package com.duke.elliot.youtubediary.main

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.duke.elliot.youtubediary.drawer.DrawerMenuUtil
import com.duke.elliot.youtubediary.drawer.DrawerMenuUtil.restoreNightMode
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        PACKAGE_NAME = this.packageName
        primaryThemeColor = DrawerMenuUtil.restoreThemeColor(this)
        AppCompatDelegate.setDefaultNightMode(restoreNightMode(this))
    }

    companion object {
        private var firebaseAuth: FirebaseAuth? = null
        var primaryThemeColor = 0
        var PACKAGE_NAME: String? = null

        fun getFirebaseAuthInstance(): FirebaseAuth {
            synchronized(this) {
                var instance = this.firebaseAuth

                if (instance == null) {
                    instance = FirebaseAuth.getInstance()
                    firebaseAuth = instance
                }

                return instance
            }
        }
    }
}