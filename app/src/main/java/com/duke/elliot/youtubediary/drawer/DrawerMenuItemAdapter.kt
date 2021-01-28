package com.duke.elliot.youtubediary.drawer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.databinding.ItemDrawerMenuContentBinding
import com.duke.elliot.youtubediary.databinding.ItemDrawerMenuDividerBinding
import com.duke.elliot.youtubediary.databinding.ItemDrawerMenuListBinding
import com.duke.elliot.youtubediary.databinding.ItemDrawerMenuSubtitleBinding
import java.lang.IllegalArgumentException

const val VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT = 1605
const val VIEW_TYPE_DRAWER_MENU_ITEM_SUBTITLE = 1606
const val VIEW_TYPE_DRAWER_MENU_ITEM_DIVIDER = 1607
const val VIEW_TYPE_DRAWER_MENU_ITEM_LIST = 1608

class DrawerMenuItemAdapter(private val drawerMenuItemDrawers: ArrayList<DrawerAdapterItem>):
    RecyclerView.Adapter<DrawerMenuItemAdapter.ViewHolder>() {

    inner class ViewHolder constructor(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(drawerAdapterItem: DrawerAdapterItem) {
            when(binding) {
                is ItemDrawerMenuContentBinding -> {
                    val drawerMenuItem = drawerAdapterItem as DrawerAdapterItem.DrawerMenuItemDrawer
                    binding.title.text = drawerMenuItem.title
                    drawerMenuItem.description?.let { description ->
                        binding.description.text = description
                    } ?: run {
                        (binding as ItemDrawerMenuContentBinding).description.visibility = View.GONE
                    }
                    drawerMenuItem.iconResourceId?.let { iconResourceId ->
                        binding.icon.setImageResource(iconResourceId)
                    } ?: run {
                        (binding as ItemDrawerMenuContentBinding).icon.visibility = View.INVISIBLE
                    }
                    binding.root.setOnClickListener {
                        drawerMenuItem.onClickListener?.invoke()
                    }

                    if (drawerMenuItem.iconResourceId != null)
                        drawerMenuItem.iconColor?.let {
                            binding.icon.setColorFilter(it)
                        }
                }
                is ItemDrawerMenuSubtitleBinding -> {
                    val drawerMenuItem = drawerAdapterItem as DrawerAdapterItem.DrawerMenuItemDrawer
                    binding.subtitle.text = drawerMenuItem.title
                }
                is ItemDrawerMenuDividerBinding -> { /** Divider */ }
                is ItemDrawerMenuListBinding -> {
                    val drawerMenuListItem = drawerAdapterItem as DrawerAdapterItem.DrawerMenuListItemDrawer<*>

                    binding.textHeader.text = drawerMenuListItem.title
                    drawerMenuListItem.iconResourceId?.let { binding.imageHeader.setImageResource(it) }
                    drawerMenuListItem.iconColor?.let { binding.imageHeader.setColorFilter(it) }
                    drawerMenuListItem.arrowDropDownColor?.let { binding.imageArrowDropDown.setColorFilter(it) }
                    drawerMenuListItem.addIconResourceId?.let { binding.imageNeutral.setImageResource(it) }
                    drawerMenuListItem.addIconColor?.let { binding.imageNeutral.setColorFilter(it) }
                    drawerMenuListItem.textShowAll?.let { binding.textShowAll.text = it }
                    drawerMenuListItem.showAllIconResourceId?.let { binding.imageShowAll.setImageResource(it) }
                    drawerMenuListItem.showAllIconColor?.let { binding.imageShowAll.setColorFilter(it) }

                    binding.linearLayoutHeader.setOnClickListener {
                        drawerMenuListItem.onHeaderClick?.invoke(binding)
                    }

                    binding.imageNeutral.setOnClickListener {
                        drawerAdapterItem.onAddIconClick?.invoke()
                    }

                    binding.recyclerView.apply {
                        layoutManager = LinearLayoutManager(this.context)
                        adapter = drawerMenuListItem.adapter
                    }

                    binding.constraintLayoutShowAll.setOnClickListener {
                        drawerMenuListItem.onShowAllClick?.invoke()
                    }
                }
            }
        }
    }

    private fun from(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when(viewType) {
            VIEW_TYPE_DRAWER_MENU_ITEM_CONTENT -> ItemDrawerMenuContentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            VIEW_TYPE_DRAWER_MENU_ITEM_SUBTITLE -> ItemDrawerMenuSubtitleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            VIEW_TYPE_DRAWER_MENU_ITEM_DIVIDER -> ItemDrawerMenuDividerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            VIEW_TYPE_DRAWER_MENU_ITEM_LIST -> ItemDrawerMenuListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            else -> throw IllegalArgumentException("Invalid viewType.")
        }

        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int = drawerMenuItemDrawers[position].viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(drawerMenuItemDrawers[position])
    }

    override fun getItemCount(): Int = drawerMenuItemDrawers.count()

    fun getItemById(id: Long) = drawerMenuItemDrawers.find { it.id == id }

    fun update(drawerAdapterItem: DrawerAdapterItem) {
        val position = drawerMenuItemDrawers.indexOf(
            drawerMenuItemDrawers.find { it.id == drawerAdapterItem.id }
        )
        drawerMenuItemDrawers[position] = drawerAdapterItem
        notifyItemChanged(position)
    }
}

sealed class DrawerAdapterItem {
    class DrawerMenuItemDrawer (
        override val id: Long,
        override val viewType: Int,
        var title: String? = null,
        var description: String? = null,
        var iconResourceId: Int? = null,
        var iconColor: Int? = null,
        var onClickListener: (() -> Unit)? = null
    ): DrawerAdapterItem()

    class DrawerMenuListItemDrawer<T> (
        override val id: Long,
        override val viewType: Int,
        var title: String? = null,
        @DrawableRes var iconResourceId: Int? = null,
        var adapter: ListAdapter<T, RecyclerView.ViewHolder>? = null,
        @ColorInt var iconColor: Int? = null,
        @ColorInt var arrowDropDownColor: Int? = null,
        var onHeaderClick: ((ItemDrawerMenuListBinding) -> Unit)? = null,
        @DrawableRes var addIconResourceId: Int? = null,
        @ColorInt var addIconColor: Int? = null,
        var onAddIconClick: (() -> Unit)? = null,
        var textShowAll: String? = null,
        @DrawableRes var showAllIconResourceId: Int? = null,
        @ColorInt var showAllIconColor: Int? = null,
        var onShowAllClick: (() -> Unit)? = null,
    ): DrawerAdapterItem()

    abstract val id: Long
    abstract val viewType: Int
}