package com.duke.elliot.youtubediary.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.databinding.FragmentSimpleDialogBinding
import kotlinx.android.synthetic.main.item_simple.view.*

class SimpleDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentSimpleDialogBinding
    private var title: String? = null
    private var simpleItems: ArrayList<SimpleItem> = arrayListOf()
    private var onItemSelectedListener: ((DialogFragment, SimpleItem) -> Unit)? = null

    fun setTitle(title: String) {
        this.title = title
    }

    fun setItems(simpleItems: ArrayList<SimpleItem>) {
        this.simpleItems = simpleItems
    }

    fun setOnItemSelectedListener(onItemSelectedListener: (DialogFragment, SimpleItem) -> Unit) {
        this.onItemSelectedListener = onItemSelectedListener
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
            adapter = ItemAdapter()
        }

        return binding.root
    }

    private inner class ItemAdapter: RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
        inner class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

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