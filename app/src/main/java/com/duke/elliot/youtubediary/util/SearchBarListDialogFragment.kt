package com.duke.elliot.youtubediary.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.annotation.ColorInt
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.databinding.FragmentSearchBarListDialogBinding
import kotlinx.android.synthetic.main.item_list.view.*
import timber.log.Timber

open class SearchBarListDialogFragment<T>: DialogFragment() {

    protected lateinit var binding: FragmentSearchBarListDialogBinding

    protected val list = arrayListOf<T>()
    protected var listAdapter: ListItemAdapter? = null

    var moreOptionsEnabled = true
    var moreOptionsMenuRes = -1
    var type = TEXT  // Default: TEXT
    var title = ""
    protected var searchWord = ""

    protected var onClickListener: OnClickListener<T>? = null
    protected var listItemDiffCallback: DiffUtil.ItemCallback<T>? = null

    interface OnClickListener<T> {
        fun onListItemClick(listItem: T)
        fun onAddClick()
        fun moreOptionsClick(itemId: Int, listItem: T)
    }

    interface FragmentContainer {
        fun <T> onRequestListener(): OnClickListener<T>?
    }

    /** importListExternally must be changed before calling onCreateView in the subclass. */
    protected var importListExternally = true

    override fun onAttach(context: Context) {
        if (context is FragmentContainer)
            onClickListener = context.onRequestListener()

        super.onAttach(context)
    }

    @Suppress("unused")
    fun setList(list: List<T>) {
        this.list.addAll(list)
        listAdapter = ListItemAdapter()
        listAdapter?.submitList(this.list)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogFragmentAnimation
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBarListDialogBinding.inflate(inflater, container, false)
        binding.textTitle.text = title

        binding.imageAdd.setOnClickListener {
            onClickListener?.onAddClick()
        }

        initSearchView()

        if (importListExternally)
            initRecyclerView()

        return binding.root
    }

    private fun initSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                listAdapter?.getFilter()?.filter(newText)
                return true
            }
        })

        binding.searchView.setOnQueryTextFocusChangeListener { _, b ->
            if (b) {
                if (binding.textTitle.isVisible)
                    binding.textTitle.visibility = View.GONE
            } else {
                binding.textTitle.visibility = View.VISIBLE
                binding.searchView.isIconified = true
            }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerViewListItem.apply {
            layoutManager = LinearLayoutManager(this.context)
            listAdapter?.let { adapter = listAdapter }
        }
    }

    protected open fun filtering(listItem: T): T? {
        if (listItem is ListItem)
            if (listItem.name.contains(searchWord))
                return listItem

        return null
    }

    protected open fun bind(holder: ListItemAdapter.ViewHolder, listItem: T) {
        if (listItem is ListItem) {
            holder.view.linearLayout_listItem.setOnClickListener {
                onClickListener?.onListItemClick(listItem)
            }

            setTextAndChangeSearchWordColor(
                holder.view.text_name,
                listItem.name,
                searchWord,
                listItem.color
            )

            when (type) {
                COLOR_BAR -> {
                    holder.view.image.visibility = View.GONE
                    holder.view.view_colorBar.visibility = View.VISIBLE
                    listItem.color?.let { holder.view.view_colorBar.setBackgroundColor(it) }
                        ?: run {
                            holder.view.view_colorBar.visibility = View.GONE
                        }
                }
                IMAGE -> {
                    holder.view.image.visibility = View.VISIBLE
                    holder.view.view_colorBar.visibility = View.GONE

                    listItem.imageUri?.let {
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
                }
            }

            if (moreOptionsEnabled) {
                holder.view.image_more.visibility = View.VISIBLE
                holder.view.image_more.setOnClickListener {
                    showPopupMenu(it, listItem)
                }
            } else
                holder.view.image_more.visibility = View.GONE
        }
    }

    /** Adapter */
    inner class ListItemAdapter: ListAdapter<T, ListItemAdapter.ViewHolder>(
        listItemDiffCallback ?: ListItemDiffCallback()
    ) {

        private val originalListItems = arrayListOf<T>()

        inner class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_list,
                parent,
                false
            )

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            bind(holder, getItem(position))
        }

        fun submitListItems(listItems: List<T>) {
            originalListItems.clear()
            originalListItems.addAll(listItems)
            submitList(listItems)
        }

        fun getFilter(): Filter {
            var listItemsFiltered: MutableList<T>

            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    searchWord = charSequence.toString()
                    listItemsFiltered =
                        if (searchWord.isBlank())
                            originalListItems
                        else {
                            val listItemsFiltering = mutableListOf<T>()

                            for (listItem in originalListItems)
                                filtering(listItem)?.let { listItemsFiltering.add(it) }

                            listItemsFiltering
                        }

                    return FilterResults().apply {
                        values = listItemsFiltered
                    }
                }

                override fun publishResults(charSequence: CharSequence?, results: FilterResults?) {
                    @Suppress("UNCHECKED_CAST")
                    if (results?.values != null) {
                        submitList(results.values as List<T>)

                        if (searchWord.isBlank())
                            notifyDataSetChanged()
                    }
                }
            }
        }
    }

    inner class ListItemDiffCallback: DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }
    }

    protected fun showPopupMenu(view: View, listItem: T) {
        val popupMenu = PopupMenu(view.context, view)

        if (moreOptionsMenuRes == -1) {
            Timber.e(IllegalArgumentException("To use moreOptions, you need to set the moreOptionsMenuRes."))
            return
        }

        popupMenu.inflate(moreOptionsMenuRes)
        popupMenu.setOnMenuItemClickListener { item ->
            onClickListener?.moreOptionsClick(item.itemId, listItem)
            true
        }

        popupMenu.show()
    }

    private fun MutableSet<Int>.notContains(element: Int) = !contains(element)

    companion object {
        // Type
        const val COLOR_BAR = 0
        const val IMAGE = 1
        const val TEXT = 2
    }
}

// Default list item.
data class ListItem(
    val id: Long,
    val name: String,
    val imageUri: String?,
    @ColorInt val color: Int?
)