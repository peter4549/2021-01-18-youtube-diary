package com.duke.elliot.youtubediary.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.duke.elliot.youtubediary.base.BaseFragment
import com.duke.elliot.youtubediary.databinding.FragmentDiariesBinding
import com.duke.elliot.youtubediary.diary_writing.DiaryWritingActivity

class DiariesFragment: BaseFragment() {

    private lateinit var binding: FragmentDiariesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiariesBinding.inflate(inflater, container, false)

        binding.floatingActionButtonAdd.setOnClickListener {
            val intent = Intent(requireContext(), DiaryWritingActivity::class.java)
            intent.putExtra(DiaryWritingActivity.EXTRA_NAME_DIARY_WRITING_ACTIVITY, DiaryWritingActivity.CREATE)
            startActivityForResult(intent, REQUEST_CODE)
        }

        return binding.root
    }

    companion object {
        const val REQUEST_CODE = 1847
    }
}