package com.duke.elliot.youtubediary.diary_writing.youtube.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.youtube.DisplayChannelModel
import com.duke.elliot.youtubediary.databinding.ItemChannelBinding

class ChannelAdapter: ListAdapter<DisplayChannelModel, RecyclerView.ViewHolder>(ChannelDiffCallback()) {

    private lateinit var recyclerView: RecyclerView
    private var onItemClickListener: OnItemClickListener? = null
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onClick(channelId: String)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    inner class ViewHolder constructor(private val binding: ItemChannelBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: DisplayChannelModel) {
            binding.root.setOnClickListener {
                onItemClickListener?.onClick(channel.id)
            }

            binding.textTitle.text = channel.title
            Glide.with(binding.root.context)
                .load(channel.thumbnailUri)
                .centerCrop()
                .error(R.drawable.ic_sharp_error_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .transform(CircleCrop())
                .into(binding.imageThumbnail)
        }
    }

    private fun from(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return from(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.bind(getItem(position))
            }
        }
    }
}

class ChannelDiffCallback: DiffUtil.ItemCallback<DisplayChannelModel>() {
    override fun areItemsTheSame(oldItem: DisplayChannelModel, newItem: DisplayChannelModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DisplayChannelModel, newItem: DisplayChannelModel): Boolean {
        return oldItem == newItem
    }
}