package com.duke.elliot.youtubediary.diary_writing.youtube.videos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.DisplayVideoModel
import com.duke.elliot.youtubediary.databinding.ItemVideoBinding

// TODO change to list adapter.
class VideoAdapter(private val videos: ArrayList<DisplayVideoModel> = arrayListOf()):
    RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

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

    fun submitList(arrayList: ArrayList<DisplayVideoModel>) {
        if (arrayList.isNotEmpty()) {
            recyclerView?.scheduleLayoutAnimation()
            val currentItemCount = itemCount
            var count = 0

            for (video in arrayList) {
                if (videos.contains(video))
                    continue

                videos.add(video)
                ++count
            }

            notifyItemRangeInserted(currentItemCount, count)
        }
    }

    fun clear() {
        recyclerView?.scheduleLayoutAnimation()
        videos.clear()
        notifyDataSetChanged()
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.count()

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