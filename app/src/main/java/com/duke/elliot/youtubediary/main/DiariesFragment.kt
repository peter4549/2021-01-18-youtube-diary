package com.duke.elliot.youtubediary.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.base.BaseFragment
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.databinding.FragmentDiariesDrawerBinding
import com.duke.elliot.youtubediary.diary_writing.DiaryWritingActivity
import com.duke.elliot.youtubediary.drawer.*
import com.duke.elliot.youtubediary.drawer.DrawerMenuUtil.restoreNightMode
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment.Companion.MODE_ADD
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment.Companion.MODE_EDIT
import com.duke.elliot.youtubediary.folder.EditFolderDialogFragment.Companion.POSITION_UNINITIALIZED
import com.duke.elliot.youtubediary.folder.FolderAdapter
import com.duke.elliot.youtubediary.splash.SplashActivity
import com.duke.elliot.youtubediary.util.*
import com.mxn.soul.flowingdrawer_core.ElasticDrawer
import com.mxn.soul.flowingdrawer_core.ElasticDrawer.OnDrawerStateChangeListener
import kotlinx.android.synthetic.main.fragment_diaries.view.*
import kotlinx.android.synthetic.main.layout_navigation_drawer.view.*
import kotlinx.coroutines.*

const val DRAWER_MENU_ITEM_ID_THEME_COLOR = 148L
const val DRAWER_MENU_ITEM_ID_THEME_NIGHT_MODE = 149L

class DiariesFragment: BaseFragment(), DiaryAdapter.OnItemClickListener {

    private lateinit var binding: FragmentDiariesDrawerBinding
    private lateinit var viewModel: DiariesViewModel

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var diaryAdapter: DiaryAdapter
    private lateinit var drawerMenuItemAdapter: DrawerMenuItemAdapter

    /** FolderAdapter */
    private val folderAdapter = FolderAdapter().apply {
        setOnClickListener(object : FolderAdapter.OnClickListener {
            override fun onEditClick(folder: Folder, position: Int) {
                showEditFolderDialog(MODE_EDIT, position, folder)
            }

            override fun onDeleteClick(folder: Folder) {
                coroutineScope.launch {
                    if (viewModel.deleteFolder(folder)) {
                        if (viewModel.currentFolder?.id == folder.id) {
                            binding.diariesFragment.text_folder.text = getString(R.string.show_all)
                            diaryAdapter.filterDiariesInFolder(null, requireContext())
                        }
                    } else
                        showToast(getString(R.string.failed_to_delete_the_folder))
                }
            }

            override fun onItemClick(folder: Folder) {
                viewModel.currentFolder = folder
                binding.diariesFragment.text_folder.text = folder.name
                diaryAdapter.filterDiariesInFolder(folder, requireContext())
            }
        })
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiariesDrawerBinding.inflate(inflater, container, false)

        val viewModelFactory = DiariesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(viewModelStore, viewModelFactory)[DiariesViewModel::class.java]

        setDisplayHomeAsUpEnabled(binding.diariesFragment.toolbar, R.drawable.ic_baseline_menu_24) {
            binding.flowingDrawer.toggleMenu(true)
        }

        initFlowingDrawer()

        diaryAdapter = DiaryAdapter().apply {
            setOnClickListener(this@DiariesFragment)
        }

        binding.diariesFragment.recyclerView_diary.adapter = diaryAdapter

        /** LiveData */
        viewModel.diaries.observe(viewLifecycleOwner, { diaries ->
            diaryAdapter.addDateAndSubmitList(diaries, requireContext())
            binding.layoutNavigationDrawer.text_count.text = diaries.count().toString()
        })

        viewModel.folders.observe(viewLifecycleOwner, { folders ->
            folderAdapter.addEmptyMessageAndSubmitList(folders.toMutableList())
            //folderAdapter.notifyDataSetChanged()
        })

        binding.diariesFragment.floatingActionButton_add.setOnClickListener {
            val intent = Intent(requireContext(), DiaryWritingActivity::class.java)
            intent.putExtra(DiaryWritingActivity.EXTRA_NAME_MODE, DiaryWritingActivity.MODE_CREATE)
            startActivityForResult(intent, REQUEST_CODE)
        }

        setAuthStateListener()

        return binding.root
    }

    private fun initFlowingDrawer() {
        binding.flowingDrawer.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL)
        binding.flowingDrawer.setOnDrawerStateChangeListener(object : OnDrawerStateChangeListener {
            override fun onDrawerStateChange(oldState: Int, newState: Int) {}
            override fun onDrawerSlide(openRatio: Float, offsetPixels: Int) {}
        })

        val iconColor = ContextCompat.getColor(requireContext(), R.color.color_icon)

        /** Version */
        val currentVersion = getVersionName(requireContext())
        val versionText = if (currentVersion == SplashActivity.latestVersionName) {
            currentVersion + " ${getString(R.string.using_the_latest_version)}"
        } else
            currentVersion + " ${getString(R.string.latest_version)}: ${SplashActivity.latestVersionName}"

        drawerMenuItemAdapter = DrawerMenuItemAdapter(
            arrayListOf(
                DrawerAdapterItem.DrawerMenuListItemDrawer(
                        id = 0L,
                        viewType = VIEW_TYPE_DRAWER_MENU_ITEM_LIST,
                        title = getString(R.string.folder),
                        iconResourceId = R.drawable.ic_round_folder_24,
                        iconColor = ContextCompat.getColor(requireContext(), R.color.color_icon),
                        neutralIconColor = iconColor,
                        neutralIconResourceId = R.drawable.ic_round_create_new_folder_24,
                        onNeutralIconClick = {
                            showEditFolderDialog(MODE_ADD, POSITION_UNINITIALIZED, null)
                        },
                        adapter = folderAdapter,
                        onHeaderClick = { binding ->
                            if (binding.linearLayoutBody.isVisible) {
                                binding.imageArrowDropDown.rotate(180F, 200)
                                binding.linearLayoutBody.collapse(0)
                            } else {
                                binding.imageArrowDropDown.rotate(0F, 200)
                                binding.linearLayoutBody.expand()
                            }
                        },
                        textShowAll = getString(R.string.show_all),
                        onTextShowAllClick = {
                            binding.diariesFragment.text_folder.text = getString(R.string.show_all)
                            diaryAdapter.filterDiariesInFolder(null, requireContext())
                        }
                ),
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    1L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_DIVIDER
                ),
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    2L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_SUBTITLE,
                    title = getString(R.string.theme)
                ),
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    DRAWER_MENU_ITEM_ID_THEME_COLOR,
                    VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT,
                    title = getString(R.string.theme_color),
                    iconResourceId = R.drawable.ic_square_24,
                    iconColor = MainApplication.primaryThemeColor
                ) {
                    DrawerMenuUtil.showColorPicker(requireActivity()) { color ->
                        val item =
                            drawerMenuItemAdapter.getItemById(DRAWER_MENU_ITEM_ID_THEME_COLOR)
                        item?.let {
                            (it as DrawerAdapterItem.DrawerMenuItemDrawer).iconColor = color
                            drawerMenuItemAdapter.update(it)
                            setColor()
                        }
                    }
                },
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    DRAWER_MENU_ITEM_ID_THEME_NIGHT_MODE,
                    VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT,
                    title = getString(R.string.night_mode),
                    description = when (restoreNightMode(requireContext())) {
                        AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_theme)
                        AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_theme)
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.system_default_theme)
                        else -> throw IllegalArgumentException("Invalid night mode.")
                    },
                    iconResourceId = R.drawable.ic_day_and_night_60px,
                    iconColor = iconColor
                ) {
                    DrawerMenuUtil.getNightModePicker(requireContext()) { nightMode, nightModeString ->
                        val item = drawerMenuItemAdapter.getItemById(
                            DRAWER_MENU_ITEM_ID_THEME_NIGHT_MODE
                        )
                        item?.let {
                            AppCompatDelegate.setDefaultNightMode(nightMode)
                            (it as DrawerAdapterItem.DrawerMenuItemDrawer).description = nightModeString
                            drawerMenuItemAdapter.update(it)
                        }
                    }.show(requireActivity().supportFragmentManager, null)
                },
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    4L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_SUBTITLE,
                    title = getString(R.string.share_the_app)
                ),
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    5L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT,
                    title = getString(R.string.share_the_app),
                    iconResourceId = R.drawable.ic_baseline_share_24,
                    iconColor = iconColor
                ) {
                    shareApplication(requireContext())
                },
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    6L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_SUBTITLE,
                    title = getString(R.string.version)
                ),
                DrawerAdapterItem.DrawerMenuItemDrawer(
                    7L,
                    VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT,
                    title = getString(R.string.version),
                    description = versionText,
                    iconResourceId = R.drawable.ic_versions_48px,
                    iconColor = iconColor
                ) {
                    if (currentVersion != SplashActivity.latestVersionName)
                        showUpdateRequestDialog()
                }
            )
        )
        binding.layoutNavigationDrawer.drawerRecyclerView.adapter = drawerMenuItemAdapter
    }

    private fun setColor() {
        binding.diariesFragment.toolbar.setBackgroundColor(MainApplication.primaryThemeColor)
        // TODO, fab update
    }

    private fun showEditFolderDialog(mode: Int, position: Int, folder: Folder?) {
        val editFolderDialog = EditFolderDialogFragment().apply {
            setMode(mode)
            setPosition(position)
            folder?.let { setFolder(it) }

            setNotifyItemChanged {
                folderAdapter.notifyItemChanged(it)
            }
        }

        editFolderDialog.let {
            it.show((requireActivity() as MainActivity).supportFragmentManager, it.tag)
        }
    }

    companion object {
        const val REQUEST_CODE = 1847
    }

    /** Version */
    private fun showUpdateRequestDialog() {
        showMaterialAlertDialog(
            requireContext(),
            title = getString(R.string.update_request_title),
            message = getString(R.string.update_request_message),
            neutralButtonText = getString(R.string.cancel),
            neutralButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
            },
            negativeButtonText = getString(R.string.afterwards),
            negativeButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
            },
            positiveButtonText = getString(R.string.update),
            positiveButtonClickListener = { dialogInterface, _ ->
                goToPlayStore(requireContext())
                dialogInterface?.dismiss()
            }
        )
    }

    /** DiaryAdapter.OnItemClickListener */
    override fun onItemClick(item: AdapterItem) {
        val diary = (item as AdapterItem.DiaryItem).diary
        val intent = Intent(requireContext(), DiaryWritingActivity::class.java)

        intent.putExtra(DiaryWritingActivity.EXTRA_NAME_MODE, DiaryWritingActivity.MODE_EDIT)
        intent.putExtra(DiaryWritingActivity.EXTRA_NAME_DIARY, diary)
        startActivityForResult(intent, REQUEST_CODE)
    }

    private fun setAuthStateListener() {
        MainApplication.getFirebaseAuthInstance().addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                binding.layoutNavigationDrawer.text_account.text = firebaseAuth.currentUser?.email
            }
        }
    }
}