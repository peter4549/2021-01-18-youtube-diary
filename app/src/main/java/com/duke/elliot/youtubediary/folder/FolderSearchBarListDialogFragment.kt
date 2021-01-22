package com.duke.elliot.youtubediary.folder

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.util.ListItem
import com.duke.elliot.youtubediary.util.SearchBarListDialogFragment

class FolderSearchBarListDialogFragment(context: Context): SearchBarListDialogFragment() {

    private val folderDao =  AppDatabase.getInstance(context).folderDao()

    init {
        importListExternally = false  // If false, recyclerViewListItem will not be initialized.
        type = COLOR_BAR
        title = context.getString(R.string.add_folder)
    }

    /** binding must be referenced after calling super.onCreateView. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) // Must be called.

        folderDao.getAll().observe(viewLifecycleOwner, { folders ->
            val listItems = folders.map { it.toListItem() } as ArrayList

            if (listAdapter == null) {
                listAdapter = ListItemAdapter()
                binding.recyclerViewListItem.apply {
                    layoutManager = LinearLayoutManager(root.context)
                    adapter = listAdapter
                }
            }

            listAdapter?.submitListItems(listItems)
        })

        return root
    }

    private fun Folder.toListItem() = ListItem(
            id = this.id,
            name = this.name,
            imageUri = null,
            color = this.color
    )
}