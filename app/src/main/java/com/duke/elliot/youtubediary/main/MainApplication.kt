package com.duke.elliot.youtubediary.main

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        private var firebaseAuth: FirebaseAuth? = null

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