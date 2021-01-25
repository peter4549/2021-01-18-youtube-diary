package com.duke.elliot.youtubediary.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}