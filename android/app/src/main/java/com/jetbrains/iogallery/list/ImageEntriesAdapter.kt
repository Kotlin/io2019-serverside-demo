package com.jetbrains.iogallery.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.model.ImageEntry
import com.jetbrains.iogallery.support.picasso
import timber.log.Timber

class ImageEntriesAdapter(
    context: Context,
    private val navController: NavController,
    private val itemMultiSelectCallback: (selectedItems: List<ImageEntry>) -> Unit
) : RecyclerView.Adapter<ImageEntriesAdapter.ImageEntryViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    private var items: List<Selectable<ImageEntry>> = emptyList()
    private var isMultiSelecting: Boolean = false

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageEntryViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_gallery_item, parent, false)
        return ImageEntryViewHolder(itemView)
    }

    override fun getItemCount() = items.count()

    override fun getItemId(position: Int) = items[position].item.id

    override fun onBindViewHolder(holder: ImageEntryViewHolder, position: Int) {
        val item = items[position]
        loadImage(item.item.url, holder.imageView)

        holder.itemView.setOnClickListener {
            if (isMultiSelecting) {
                onItemLongClicked(item.item, !items[position].selected)
            } else {
                onItemClicked(item.item)
            }
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(item.item, !items[position].selected)
            true
        }

        holder.itemView.isSelected = item.selected
        holder.checkImageView.isVisible = item.selected
        holder.imageView.scaleX = if (item.selected) .9f else 1f
        holder.imageView.scaleY = if (item.selected) .9f else 1f
    }

    private fun loadImage(imageUrl: String, targetView: ImageView) {
        targetView.context.picasso()
            .load("$imageUrl?w=500&h=500&fm=webp".toUri())
            .fit()
            .centerCrop()
            .placeholder(R.drawable.loading_placeholder)
            .error(R.drawable.broken_placeholder)
            .into(targetView)
    }

    private fun onItemClicked(item: ImageEntry) {
        navController.navigate(ListFragmentDirections.actionListFragmentToDetailFragment(item.id))
    }

    private fun onItemLongClicked(item: ImageEntry, selected: Boolean) {
        val changedIndex = items.indexOfFirst { it.item == item }
        if (changedIndex < 0) {
            Timber.e("Item $item has been long pressed but I cannot find it!")
            return
        }

        val countAfterChanged = if (changedIndex < items.size - 1) items.count() - changedIndex - 1 else 0

        items = items.take(changedIndex) +
            items[changedIndex].copy(selected = selected) +
            items.takeLast(countAfterChanged)

        notifyItemChanged(changedIndex, WhatChanged(selected = true))    // TODO use payload!

        val selectedItems = items.filter { it.selected }
        isMultiSelecting = selectedItems.isNotEmpty()
        itemMultiSelectCallback(selectedItems.map { it.item })
    }

    fun replaceItemsWith(items: List<ImageEntry>) {
        this.items = items.map { Selectable(it) }
        notifyDataSetChanged() // TODO use diffutils
    }

    fun cancelMultiselect() {
        items.filter { it.selected }
            .forEach { onItemLongClicked(it.item, false) }
    }

    class ImageEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imageView = view.findViewById<ImageView>(R.id.itemImage)!!
        val checkImageView = view.findViewById<ImageView>(R.id.itemCheckedImage)!!
    }

    private data class Selectable<T>(val item: T, val selected: Boolean = false)

    private data class WhatChanged(val selected: Boolean = false)
}
