package com.duke.elliot.youtubediary.util

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.TextView
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
import com.duke.elliot.youtubediary.databinding.FragmentSearchBarListDialogBinding
import kotlinx.android.synthetic.main.item_list.view.*


open class SearchBarListDialogFragment: DialogFragment() {

    protected lateinit var binding: FragmentSearchBarListDialogBinding

    protected val list = arrayListOf<ListItem>()
    protected var listAdapter: ListItemAdapter? = null

    private var imageAddCallback: (() -> Unit)? = null
    private var moreOptionsItemIdOnSelectedPairs = mutableMapOf<Int, (ListItem) -> Unit>()
    var moreOptionsEnabled = true
    var moreOptionsMenuRes = -1
    var type = TEXT  // Default: TEXT
    var title = ""
    private var searchWord = ""

    /** importListExternally must be changed before calling onCreateView in the subclass. */
    protected var importListExternally = true

    @Suppress("unused")
    fun setOnMoreOptionsItemSelectedListeners(vararg moreOptionsItemIdOnSelectedListenerPairs: Pair<Int, (ListItem) -> Unit>) {
        moreOptionsItemIdOnSelectedListenerPairs.forEach {
            if (this.moreOptionsItemIdOnSelectedPairs.keys.notContains(it.first))
                this.moreOptionsItemIdOnSelectedPairs[it.first] = it.second
        }
    }

    @Suppress("unused")
    fun setList(list: List<ListItem>) {
        this.list.addAll(list)
        listAdapter = ListItemAdapter()
        listAdapter?.submitList(this.list)
    }

    fun setImageAddCallback(imageAddCallback: () -> Unit) {
        this.imageAddCallback = imageAddCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBarListDialogBinding.inflate(inflater, container, false)
        binding.textTitle.text = title

        initSearchView()

        binding.imageAdd.setOnClickListener {
            imageAddCallback?.invoke()
        }

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

    inner class ListItemAdapter: ListAdapter<ListItem, ListItemAdapter.ViewHolder>(
        ListItemDiffCallback()
    ) {

        private val originalListItems = arrayListOf<ListItem>()

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
            val listItem = getItem(position)

            holder.view.text_name.text = listItem.name
            setTextAndChangeSearchWordColor(holder.view.text_name, listItem.name, searchWord, listItem.color)

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

        fun submitListItems(listItems: List<ListItem>) {
            originalListItems.clear()
            originalListItems.addAll(listItems)
            submitList(listItems)
        }

        fun getFilter(): Filter {
            var listItemsFiltered: MutableList<ListItem>

            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    searchWord = charSequence.toString()
                    listItemsFiltered = if (searchWord.isBlank())
                        originalListItems
                    else {
                        val listItemsFiltering = mutableListOf<ListItem>()

                        for (listItem in originalListItems) {
                            if (listItem.name.contains(searchWord))
                                listItemsFiltering.add(listItem)
                        }

                        listItemsFiltering
                    }

                    return FilterResults().apply {
                        values = listItemsFiltered
                    }
                }

                override fun publishResults(charSequence: CharSequence?, results: FilterResults?) {
                    @Suppress("UNCHECKED_CAST")
                    if (results?.values != null)
                        submitList(results.values as List<ListItem>)
                }
            }
        }

        private fun setTextAndChangeSearchWordColor(
            textView: TextView, text: String,
            searchWord: String, @ColorInt color: Int?
        ) {
            if (color == null) {
                textView.text = text
                return
            }

            if (text.isBlank()) {
                textView.text = text
                return
            }

            val hexColor = color.toHexColor()
            val htmlText = text.replaceFirst(searchWord, "<font color='$hexColor'>$searchWord</font>")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                textView.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            else
                @Suppress("DEPRECATION")
                textView.text = Html.fromHtml(htmlText)
        }
    }

    protected fun showPopupMenu(view: View, listItem: ListItem) {
        val popupMenu = PopupMenu(view.context, view)

        if (moreOptionsMenuRes == -1)
            return

        popupMenu.inflate(moreOptionsMenuRes)
        popupMenu.setOnMenuItemClickListener { item ->
            moreOptionsItemIdOnSelectedPairs[item.itemId]?.invoke(listItem)
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

data class ListItem(
    val id: Long,
    val name: String,
    val imageUri: String?,
    @ColorInt val color: Int?
)

class ListItemDiffCallback: DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem == newItem
    }
}