package com.duke.elliot.youtubediary.diary_writing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.diary_writing.youtube.videos.DisplayVideoModel
import kotlinx.android.synthetic.main.item_view_pager.view.*

class ViewPagerAdapter(private val context: Context) : PagerAdapter() {

    private var items = mutableListOf<DisplayVideoModel>()

    fun setItems(items: MutableList<DisplayVideoModel>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): View {
        val item = items[position]
        val view = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.item_view_pager, collection, false)

        Glide.with(context)
            .load(item.thumbnailUri)
            .centerCrop()
            .error(R.drawable.ic_sharp_error_24)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view.image_thumbnail)

        collection.addView(view)
        return view
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int = items.count()

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    /*
    override fun notifyDataSetChanged() {
        var key = 0
        for (i in 0 until items.count())
        super.notifyDataSetChanged()
    }

    @Override
    public void notifyDataSetChanged() {
        int key = 0;
        for(int i = 0; i < views.size(); i++) {
            key = views.keyAt(i);
            View view = views.get(key);
            <refresh view with new data>
        }
        super.notifyDataSetChanged();
    }

     */
}