package com.duke.elliot.youtubediary.folder

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.FolderDao
import com.duke.elliot.youtubediary.databinding.FragmentEditFolderDialogBinding
import com.duke.elliot.youtubediary.util.toHexColor
import kotlinx.android.synthetic.main.fragment_edit_folder_dialog.*
import kotlinx.coroutines.*
import petrov.kristiyan.colorpicker.ColorPicker
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

class EditFolderDialogFragment(private val folderDao: FolderDao, private val mode: Int, private val folder: Folder?)
    : DialogFragment() {

    init {
        if (mode == MODE_EDIT && folder == null)
            throw NullPointerException("In edit mode, the folder must not be null.")
    }

    private lateinit var binding: FragmentEditFolderDialogBinding
    private lateinit var afterEditingCallback: (Folder) -> Unit

    private var color = 0

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    fun setCallbackAfterEditing(callbackAfterEditing: (Folder) -> Unit) {
        this.afterEditingCallback = callbackAfterEditing
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_folder_dialog, container, false)
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        color = ContextCompat.getColor(requireContext(), R.color.colorRed200)  // Default color.

        binding.title.text = when(mode) {
            MODE_ADD -> getString(R.string.add_folder)
            MODE_EDIT -> getString(R.string.edit_folder)
            else -> throw IllegalArgumentException("Invalid mode value.")
        }

        if (mode == MODE_EDIT)
            binding.textInputEditTextName.setText(folder?.name)

        binding.folderColor.setCardBackgroundColor(color)
        binding.okButton.setOnClickListener {
            when(mode) {
                MODE_ADD -> createFolder()?.let {
                    insertFolder(it)
                    inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                } ?: run {
                    binding.textInputLayout.isErrorEnabled = true
                    binding.textInputLayout.error = getString(R.string.enter_the_folder_name)
                }
                MODE_EDIT -> {
                    if (binding.textInputEditTextName.text.toString().isNotBlank()) {
                        folder?.let {
                            it.name = binding.textInputEditTextName.text.toString()
                            it.color = color

                            updateFolder(it)
                            afterEditingCallback.invoke(it)
                        }

                        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                    } else {
                        binding.textInputLayout.isErrorEnabled = true
                        binding.textInputLayout.error = getString(R.string.enter_the_folder_name)
                    }
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.selectFolderColorContainer.setOnClickListener {
            val colorPicker = ColorPicker(requireActivity())
            val themeColors = requireContext().resources.getIntArray(R.array.theme_colors).toList()
            val hexColors = themeColors.map { it.toHexColor() } as ArrayList

            colorPicker.setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
                override fun onChooseColor(position: Int, color: Int) {
                    this@EditFolderDialogFragment.color = color
                    binding.folderColor.setCardBackgroundColor(this@EditFolderDialogFragment.color)
                }

                override fun onCancel() {  }
            })
                    .setTitle(getString(R.string.select_folder_color))
                    .setColumns(6)
                    .setColorButtonMargin(2, 2, 2, 2)
                    .setColorButtonDrawable(R.drawable.background_white_rounded_corners)
                    .setColors(hexColors)
                    .setDefaultColorButton(color)
                    .show()
        }

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    private fun insertFolder(folder: Folder) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                folderDao.insert(folder)
            }

            dismiss()
        }
    }

    private fun updateFolder(folder: Folder) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                folderDao.update(folder)
            }

            dismiss()
        }
    }

    private fun createFolder(): Folder? {
        val name = binding.textInputEditTextName.text.toString()

        if (name.isBlank())
            return null

        return Folder(
                name = name,
                color = color
        )
    }

    companion object {
        const val MODE_ADD = 0
        const val MODE_EDIT = 1
    }
}