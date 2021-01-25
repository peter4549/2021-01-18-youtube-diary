package com.duke.elliot.youtubediary.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.duke.elliot.youtubediary.R
import com.duke.elliot.youtubediary.database.AppDatabase
import com.duke.elliot.youtubediary.database.DEFAULT_FOLDER_ID
import com.duke.elliot.youtubediary.database.Diary
import com.duke.elliot.youtubediary.database.Folder
import com.duke.elliot.youtubediary.databinding.ItemDateBinding
import com.duke.elliot.youtubediary.databinding.ItemDiaryBinding
import com.duke.elliot.youtubediary.util.toDateFormat
import kotlinx.coroutines.*
import java.util.*
import kotlin.Comparator

const val VIEW_TYPE_DATE_ITEM = 1
const val VIEW_TYPE_DIARY_ITEM = 2

const val SORT_BY_LATEST = 0
const val SORT_BY_OLDEST = 1

class DiaryAdapter: ListAdapter<AdapterItem, DiaryAdapter.ViewHolder>(AdapterItemDiffCallback()) {

    private var onItemClickListener: OnItemClickListener? = null
    private var sortingCriteria = SORT_BY_LATEST
    private var folder: Folder? = null
    private val originalDiaries = mutableListOf<Diary>()

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    fun setOnClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(item: AdapterItem)
    }

    fun filterDiariesInFolder(folder: Folder?, context: Context) {
        this.folder = folder
        addDateAndSubmitList(originalDiaries, context, false)
    }

    fun addDateAndSubmitList(list: List<Diary>?, context: Context, updateOriginalDiaries: Boolean = true) {
        if (list.isNullOrEmpty())
            return

        if (updateOriginalDiaries) {
            originalDiaries.clear()
            originalDiaries.addAll(list)
        }

        val listInFolder = mutableListOf<Diary>()

        if (folder == null)
            listInFolder.addAll(list)
        else
            listInFolder.addAll(list.filter { it.folderId == folder?.id })

        sort(listInFolder)

        val items = mutableListOf<AdapterItem>()
        var yearMonth = ""

        for ((index, item) in listInFolder.withIndex()) {
            val new = item.updatedAt.toDateFormat(context.getString(R.string.year_month_format))

            if (new != yearMonth) {
                val isTop = yearMonth.isBlank()
                yearMonth = new
                items.add(AdapterItem.DateItem(-index.toLong(), yearMonth, isTop))
            }

            items.add(AdapterItem.DiaryItem(item))
        }

        submitList(items)
    }

    inner class ViewHolder constructor(val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(adapterItem: AdapterItem) {
            when(binding) {
                is ItemDiaryBinding -> {
                    val diary = (adapterItem as AdapterItem.DiaryItem).diary

                    binding.root.setOnClickListener {
                        onItemClickListener?.onItemClick(adapterItem)
                    }

                    binding.textDate.text = diary.updatedAt.toDateFormat("dd")
                    binding.textDayOfWeek.text = diary.updatedAt.toDateFormat("EE")

                    var title = diary.content

                    if (diary.content.length > 50)
                        title = diary.content.substring(0, 50)
                    binding.textContent.text = title

                    binding.textFolder.visibility = View.GONE

                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance(binding.root.context).folderDao().getFolderValue(diary.folderId)?.let {
                                val folderName = it.name
                                withContext(Dispatchers.Main) {
                                    binding.textFolder.visibility = View.VISIBLE
                                    binding.textFolder.text = folderName
                                }
                            }
                        }
                    }

                    if (diary.youtubeVideos.isNotEmpty()) {
                        val thumbnailUri = diary.youtubeVideos[0].thumbnailUri
                        binding.imageThumbnail.visibility = View.VISIBLE
                        Glide.with(binding.root.context)
                            .load(thumbnailUri)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .error(R.drawable.ic_sharp_error_24)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.imageThumbnail)
                    } else
                        binding.imageThumbnail.visibility = View.GONE

                }
                is ItemDateBinding -> {
                    val dateItem = (adapterItem as AdapterItem.DateItem)
                    binding.textYearMonth.text = dateItem.yearMonth

                    if (dateItem.isTop) {
                        binding.linearLayout.visibility = View.VISIBLE

                        if (sortingCriteria == SORT_BY_LATEST)
                            binding.textSort.text = binding.root.context.getString(R.string.oldest)
                        else
                            binding.textSort.text = binding.root.context.getString(R.string.latest)

                        binding.linearLayoutSort.setOnClickListener {
                            sortingCriteria =
                                    if (sortingCriteria == SORT_BY_LATEST)
                                        SORT_BY_OLDEST
                                    else
                                        SORT_BY_LATEST

                            val list = currentList.filterIsInstance<AdapterItem.DiaryItem>().map { it.diary }

                            sort(list)
                            addDateAndSubmitList(list, binding.root.context)
                            notifyDataSetChanged()
                        }
                    } else
                        binding.linearLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun from(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when(viewType) {
            VIEW_TYPE_DATE_ITEM -> ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            VIEW_TYPE_DIARY_ITEM -> ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw IllegalArgumentException("Invalid viewType.")
        }

        return ViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdapterItem.DateItem -> VIEW_TYPE_DATE_ITEM
            is AdapterItem.DiaryItem -> VIEW_TYPE_DIARY_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun sort(list: List<Diary>?) {
        list?.let {
            Collections.sort(list,
                    Comparator { o1: Diary, o2: Diary ->
                        when (sortingCriteria) {
                            SORT_BY_LATEST -> {
                                return@Comparator (o1.updatedAt - o2.updatedAt).toInt()
                            }
                            SORT_BY_OLDEST -> {
                                return@Comparator (o2.updatedAt - o1.updatedAt).toInt()
                            }
                            else -> 0
                        }
                    }
            )
        }
    }
}

class AdapterItemDiffCallback: DiffUtil.ItemCallback<AdapterItem>() {
    override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
        return oldItem == newItem
    }
}

sealed class AdapterItem {
    data class DateItem(
            override val id: Long,
            val yearMonth: String,
            val isTop: Boolean = false
    ): AdapterItem()

    data class DiaryItem(val diary: Diary): AdapterItem() {
        override val id = diary.id
    }

    abstract val id: Long
}