package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ItemVideoBinding

class VideoAdapter: ListAdapter<DisplayVideoModel, RecyclerView.ViewHolder>(VideoDiffCallback()) {

    private var onMenuItemClickListener: OnMenuItemClickListener? = null
    fun setOnMenuItemClickListener (onMenuItemClickListener: OnMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener
    }

    interface OnMenuItemClickListener {
        fun play(displayVideoModel: DisplayVideoModel)
        fun addToDiary(displayVideoModel: DisplayVideoModel)
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    inner class ViewHolder constructor(private val binding: ItemVideoBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(displayVideoModel: DisplayVideoModel) {
            binding.root.setOnClickListener {
                showPopupMenu(binding.textTitle, displayVideoModel)
            }
            Glide.with(binding.root.context)
                .load(displayVideoModel.thumbnailUri)
                .error(R.drawable.ic_sharp_error_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageThumbnail)
            binding.textTitle.text = displayVideoModel.title
            binding.textPublishedAt.text = displayVideoModel.timeAgo
        }
    }

    private fun from(parent: ViewGroup): ViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = from(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder)
            holder.bind(getItem(position))
    }

    private fun showPopupMenu(view: View, video: DisplayVideoModel) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.menu_video_adapter)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_play -> {
                    onMenuItemClickListener?.play(video)
                    true
                }
                R.id.item_addToDiary -> {
                    onMenuItemClickListener?.addToDiary(video)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }


}

class VideoDiffCallback: DiffUtil.ItemCallback<DisplayVideoModel>() {
    override fun areItemsTheSame(oldItem: DisplayVideoModel, newItem: DisplayVideoModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DisplayVideoModel, newItem: DisplayVideoModel): Boolean {
        return oldItem == newItem
    }
}