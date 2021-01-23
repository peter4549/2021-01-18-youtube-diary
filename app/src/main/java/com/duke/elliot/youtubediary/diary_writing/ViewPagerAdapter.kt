package com.duke.elliot.youtubediary.diary_writing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.android.synthetic.main.item_view_pager.view.*

class ViewPagerAdapter: RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onClick(displayVideo: DisplayVideoModel)
    }

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    private val items = arrayListOf<DisplayVideoModel>()

    fun add(position: Int = 0, item: DisplayVideoModel) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    fun addAll(items: List<DisplayVideoModel>) {
        val positionStart = itemCount
        this.items.addAll(items)
        notifyItemRangeInserted(positionStart, items.count())
    }

    fun remove(item: DisplayVideoModel) {
        val position = items.indexOf(item)
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_pager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.view.context)
            .load(item.thumbnailUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.drawable.ic_sharp_error_24)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.view.image_thumbnail)

        holder.view.setOnClickListener {
            onItemClickListener?.onClick(item)
        }
    }

    override fun getItemCount(): Int = items.count()

    fun getItem(position: Int) = items[position]
}