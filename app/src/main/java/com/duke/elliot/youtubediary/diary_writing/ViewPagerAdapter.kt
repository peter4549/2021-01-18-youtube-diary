package com.duke.elliot.youtubediary.diary_writing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.youtube.DisplayVideoModel
import kotlinx.android.synthetic.main.item_view_pager.view.*

class ViewPagerAdapter: RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    private val items = arrayListOf<DisplayVideoModel>()

    fun add(position: Int = 0, item: DisplayVideoModel) {
        items.add(position, item)
        println("AaAAA: IIII: $item")
        println("AaAAA: IIIILLLLL: $itemCount")
        // notifyDataSetChanged() test, this is work.
        notifyItemInserted(itemCount.dec())
    }

    fun addAll(items: List<DisplayVideoModel>) {
        val positionStart = itemCount
        this.items.addAll(items)
        // notifyDataSetChanged() test, this is work.
        notifyItemRangeInserted(positionStart, items.count())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_pager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        //holder.view.image_thumbnail.setImageResource(R.drawable.ic_google_sign_in_28)
        Glide.with(holder.view.context)
            .load(item.thumbnailUri)
            // .placeholder(R.drawable.ic_sharp_edit_24) // TEST
            .error(R.drawable.ic_sharp_error_24)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.view.image_thumbnail)

        holder.view.setOnClickListener {
            Toast.makeText(holder.view.context, "ACE", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = items.count()
}