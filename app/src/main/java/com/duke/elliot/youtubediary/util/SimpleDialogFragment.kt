package com.duke.elliot.youtubediary.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.databinding.FragmentSimpleDialogBinding
import kotlinx.android.synthetic.main.item_simple.view.*
import timber.log.Timber

class SimpleDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentSimpleDialogBinding
    private var title: String? = null
    private var simpleItems: ArrayList<SimpleItem> = arrayListOf()
    private var onItemSelectedListener: ((DialogFragment, SimpleItem) -> Unit)? = null
    private var simpleItemAdapter = SimpleItemAdapter()
    private var onScrollReachedBottomListener: OnScrollReachedBottomListener? = null

    fun setOnScrollReachedBottomListener(onScrollReachedBottomListener: OnScrollReachedBottomListener) {
        this.onScrollReachedBottomListener = onScrollReachedBottomListener
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun setItems(simpleItems: ArrayList<SimpleItem>) {
        this.simpleItems = simpleItems
    }

    fun setOnItemSelectedListener(onItemSelectedListener: (DialogFragment, SimpleItem) -> Unit) {
        this.onItemSelectedListener = onItemSelectedListener
    }

    interface OnScrollReachedBottomListener {
        fun onScrollReachedBottom(simpleItemAdapter: SimpleItemAdapter)
    }

    fun clear() {
        title = null
        simpleItems.clear()
        onItemSelectedListener = null
        simpleItemAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSimpleDialogBinding.inflate(inflater, container, false)
        title?.let { binding.textTitle.text = it }
        binding.recyclerViewItem.apply {
            layoutManager = LinearLayoutManager(binding.root.context)
            setHasFixedSize(true)
            adapter = simpleItemAdapter
        }

        binding.recyclerViewItem.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                Timber.d("recyclerViewVideo scrolled. dx: $dx, dy: $dy")
                val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
                val itemCount = layoutManager.itemCount
                val lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()

                if (lastCompletelyVisibleItemPosition >= itemCount.dec()) {
                    onScrollReachedBottomListener?.onScrollReachedBottom(simpleItemAdapter)
                }
            }
        })

        return binding.root
    }

    inner class SimpleItemAdapter: RecyclerView.Adapter<SimpleItemAdapter.ViewHolder>() {
        inner class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

        fun addItems(simpleItems: ArrayList<SimpleItem>) {
            val positionStart = itemCount.dec()
            this@SimpleDialogFragment.simpleItems.addAll(simpleItems)
            notifyItemRangeInserted(positionStart, simpleItems.count())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_simple,
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = simpleItems[position]
            holder.view.text_name.text = item.name
            holder.view.image.visibility = View.VISIBLE
            item.imageUri?.let {
                Glide.with(holder.view.context)
                    .load(it)
                    .centerCrop()
                    .error(R.drawable.ic_sharp_error_24)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(holder.view.image)
            } ?: run {
                holder.view.image.visibility = View.GONE
            }

            holder.view.setOnClickListener {
                onItemSelectedListener?.invoke(this@SimpleDialogFragment, item)
            }
        }

        override fun getItemCount(): Int = simpleItems.count()
    }
}

data class SimpleItem(
    val id: String,
    val name: String,
    val imageUri: String? = null
)