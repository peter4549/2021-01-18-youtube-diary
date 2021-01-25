package com.duke.elliot.youtubediary.folder

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.FolderDao
import com.duke.elliot.youtubediary.databinding.FragmentEditFolderDialogBinding
import com.duke.elliot.youtubediary.diary_writing.DiaryWritingActivity
import com.duke.elliot.youtubediary.util.isZero
import com.duke.elliot.youtubediary.util.toHexColor
import kotlinx.android.synthetic.main.fragment_edit_folder_dialog.*
import kotlinx.coroutines.*
import petrov.kristiyan.colorpicker.ColorPicker

class EditFolderDialogFragment: DialogFragment() {

    private var mode: Int = MODE_UNINITIALIZED
    private var position: Int = POSITION_UNINITIALIZED
    private var folder: Folder? = null
    private lateinit var folderDao: FolderDao

    /** notifyItemChanged */
    private var notifyItemChanged: ((position: Int) -> Unit)? = null

    fun setNotifyItemChanged(notifyItemChanged: ((position: Int) -> Unit)) {
        this.notifyItemChanged = notifyItemChanged
    }

    fun setMode(mode: Int) {
        this.mode = mode
    }

    fun setPosition(position: Int) {
        this.position = position
    }

    fun setFolder(folder: Folder) {
        this.folder = folder
    }

    private var onClickListener: OnClickListener? = null

    // UI update.
    interface OnClickListener {
        fun onPositiveButtonClick(folder: Folder)
    }

    interface FragmentContainer {
        fun onRequestOnClickListener(): OnClickListener?
    }

    private lateinit var binding: FragmentEditFolderDialogBinding

    private var color = 0
    private var typedValue = TypedValue()  // android.R.attr.selectableItemBackgroundBorderless

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_MODE, mode)
        outState.putInt(KEY_POSITION, position)
        outState.putParcelable(KEY_FOLDER, folder)
    }

    override fun onAttach(context: Context) {
        if (context is DiaryWritingActivity)
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                typedValue,
                true
            )

        if (context is FragmentContainer)
            onClickListener = context.onRequestOnClickListener()

        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogFragmentAnimation
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_folder_dialog,
            container,
            false
        )

        if (savedInstanceState != null) {
            mode = savedInstanceState.getInt(KEY_MODE)
            position = savedInstanceState.getInt(KEY_POSITION)
            folder = savedInstanceState.getParcelable(KEY_FOLDER)
        }

        if (mode == MODE_UNINITIALIZED)
            throw IllegalStateException("mode must be initialized.")

        if (mode == MODE_EDIT && folder == null)
            throw NullPointerException("In edit mode, the folder must not be null.")

        folderDao = AppDatabase.getInstance(binding.root.context).folderDao()

        color = folder?.color ?: ContextCompat.getColor(requireContext(), R.color.colorRed200)  // Default color.

        binding.title.text = when(mode) {
            MODE_ADD -> getString(R.string.add_folder)
            MODE_EDIT -> getString(R.string.edit_folder)
            else -> throw IllegalArgumentException("Invalid mode value.")
        }

        binding.cancelButton.setText(R.string.cancel)
        binding.okButton.setText(R.string.ok)

        if (mode == MODE_EDIT)
            binding.textInputEditTextName.setText(folder?.name)

        binding.folderColor.setCardBackgroundColor(color)
        binding.okButton.setOnClickListener {
            when(mode) {
                MODE_ADD -> createFolder()?.let {
                    insertFolder(it)
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
                            onClickListener?.onPositiveButtonClick(it)

                            if (position != POSITION_UNINITIALIZED)
                            notifyItemChanged?.invoke(position)
                        }
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

            val originalColor = color

            colorPicker.setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
                override fun onChooseColor(position: Int, color: Int) {
                    if (color.isZero())
                        this@EditFolderDialogFragment.color = originalColor
                    else
                        this@EditFolderDialogFragment.color = color
                    binding.folderColor.setCardBackgroundColor(this@EditFolderDialogFragment.color)
                }

                override fun onCancel() {
                    color = originalColor
                }
            }).setTitle(getString(R.string.select_folder_color))
                .setColumns(6)
                .setColorButtonMargin(2, 2, 2, 2)
                .setColorButtonDrawable(R.drawable.background_white_rounded_corners)
                .setColors(hexColors)
                .setDefaultColorButton(color)
                .show()

            colorPicker.negativeButton?.let {
                it.setText(R.string.cancel)
                it.setTextColor(getColor(it.context, R.color.color_text))
                it.setBackgroundResource(typedValue.resourceId)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    it.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
                else
                    @Suppress("DEPRECATION")
                    it.setTextAppearance(it.context, R.style.WellTodayMediumFontFamilyStyle)
            }

            colorPicker.positiveButton?.let {
                it.setText(R.string.ok)
                it.setTextColor(getColor(it.context, R.color.text_accent_light))
                it.setBackgroundResource(typedValue.resourceId)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    it.setTextAppearance(R.style.WellTodayMediumFontFamilyStyle)
                else
                    @Suppress("DEPRECATION")
                    it.setTextAppearance(it.context, R.style.WellTodayMediumFontFamilyStyle)
            }
        }

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    private fun getColor(context: Context, id: Int) = ContextCompat.getColor(context, id)

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
        const val MODE_UNINITIALIZED = -1
        const val MODE_ADD = 0
        const val MODE_EDIT = 1

        const val POSITION_UNINITIALIZED = -1

        private const val KEY_MODE = "com.duke.elliot.youtubediary.folder.edit_folder_dialog_fragment.key_mode"
        private const val KEY_FOLDER = "com.duke.elliot.youtubediary.folder.edit_folder_dialog_fragment.key_folder"
        private const val KEY_POSITION = "com.duke.elliot.youtubediary.folder.edit_folder_dialog_fragment.key_position"
    }
}