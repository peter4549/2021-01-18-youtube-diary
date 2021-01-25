package com.duke.elliot.youtubediary.folder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.databinding.ItemEmptySmallBinding
import com.duke.elliot.youtubediary.databinding.ItemFolderBinding

const val VIEW_TYPE_FOLDER_ITEM = 0
const val VIEW_TYPE_EMPTY_ITEM = 1

class FolderAdapter: ListAdapter<FolderAdapterItem, RecyclerView.ViewHolder>(FolderDiffCallback()) {

    private var onClickListener: OnClickListener? = null

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    fun addEmptyMessageAndSubmitList(list: List<Folder>?) {
        val items =
            if (list.isNullOrEmpty())
                listOf(FolderAdapterItem.EmptyItem)
            else
                list.map { FolderAdapterItem.FolderItem(it.id, it) }

        submitList(items)
    }

    interface OnClickListener {
        fun onEditClick(folder: Folder, position: Int)
        fun onDeleteClick(folder: Folder)
        fun onItemClick(folder: Folder)
    }

    inner class ViewHolder constructor(val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(folderAdapterItem: FolderAdapterItem) {
            when(binding) {
                is ItemFolderBinding -> {
                    val folder = (folderAdapterItem as FolderAdapterItem.FolderItem).folder
                    binding.color.setBackgroundColor(folder.color)
                    binding.textName.text = folder.name
                    binding.textCount.text = folder.diaryIds.count().toString()
                    binding.imageMore.setOnClickListener {
                        showPopupMenu(it, folder, adapterPosition)
                    }

                    binding.cardView.setOnClickListener {
                        onClickListener?.onItemClick(folder)
                    }
                }
                is ItemEmptySmallBinding -> {
                    binding.textEmptyMessage.text = binding.root.context.getString(R.string.no_folder)
                }
            }
        }
    }

    private fun from(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when(viewType) {
            VIEW_TYPE_FOLDER_ITEM -> ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            VIEW_TYPE_EMPTY_ITEM -> ItemEmptySmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw IllegalArgumentException("Invalid viewType.")
        }
        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FolderAdapterItem.FolderItem -> VIEW_TYPE_FOLDER_ITEM
            is FolderAdapterItem.EmptyItem -> VIEW_TYPE_EMPTY_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return from(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    private fun showPopupMenu(view: View, folder: Folder, position: Int) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.menu_folder_adapter)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_edit -> {
                    onClickListener?.onEditClick(folder, position)
                    true
                }
                R.id.item_delete -> {
                    onClickListener?.onDeleteClick(folder)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
}

class FolderDiffCallback: DiffUtil.ItemCallback<FolderAdapterItem>() {
    override fun areItemsTheSame(oldItem: FolderAdapterItem, newItem: FolderAdapterItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FolderAdapterItem, newItem: FolderAdapterItem): Boolean {
        return oldItem == newItem
    }
}

sealed class FolderAdapterItem {
    data class FolderItem(
        override val id: Long,
        val folder: Folder
    ): FolderAdapterItem()

    object EmptyItem : FolderAdapterItem() {
        override val id: Long = Long.MIN_VALUE
    }

    abstract val id: Long
}