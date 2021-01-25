package com.duke.elliot.youtubediary.folder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.database.FolderDao
import com.duke.elliot.youtubediary.util.SearchBarListDialogFragment
import com.duke.elliot.youtubediary.util.setTextAndChangeSearchWordColor
import kotlinx.android.synthetic.main.item_list.view.*
import kotlinx.coroutines.*

class FolderSearchBarListDialogFragment : SearchBarListDialogFragment<Folder>() {

    private lateinit var folderDao: FolderDao

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    init {
        importListExternally = false  // If false, recyclerViewListItem will not be initialized.
        type = COLOR_BAR
        listItemDiffCallback = FolderDiffCallback()
    }

    override fun filtering(listItem: Folder): Folder? {
        if (listItem.name.contains(searchWord))
            return listItem

        return null
    }

    override fun bind(holder: ListItemAdapter.ViewHolder, listItem: Folder) {
        holder.view.linearLayout_listItem.setOnClickListener {
            onClickListener?.onListItemClick(listItem)
            coroutineScope.launch {
                delay(150)
                dismiss()
            }
        }

        setTextAndChangeSearchWordColor(
            holder.view.text_name,
            listItem.name,
            searchWord,
            listItem.color
        )

        holder.view.image.visibility = View.GONE
        holder.view.view_colorBar.visibility = View.VISIBLE
        listItem.color.let { holder.view.view_colorBar.setBackgroundColor(it) }

        if (moreOptionsEnabled) {
            holder.view.image_more.visibility = View.VISIBLE
            holder.view.image_more.setOnClickListener {
                showPopupMenu(it, listItem)
            }
        } else
            holder.view.image_more.visibility = View.GONE

        holder.view.text_count.text = listItem.diaryIds.count().toString()
    }

    inner class FolderDiffCallback: DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }

    /** binding must be referenced after calling super.onCreateView. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) // Must be called.

        folderDao = AppDatabase.getInstance(root.context).folderDao()
        title = root.context.getString(R.string.add_folder)
        binding.textTitle.text = title
        binding.imageAdd.setImageResource(R.drawable.ic_round_create_new_folder_24)

        folderDao.getAll().observe(viewLifecycleOwner, { folders ->
            if (listAdapter == null) {
                listAdapter = ListItemAdapter()
                binding.recyclerViewListItem.apply {
                    layoutManager = LinearLayoutManager(root.context)
                    adapter = listAdapter
                }
            }

            listAdapter?.submitListItems(folders.filter { it.name.isNotBlank() })
            listAdapter?.notifyDataSetChanged()
        })

        return root
    }
}