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
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.support.picasso
import timber.log.Timber

class PhotosAdapter(
    context: Context,
    private val navController: NavController,
    private val itemMultiSelectCallback: (selectedItems: List<Photo>) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    private var items: List<Selectable<Photo>> = emptyList()
    private var isMultiSelecting: Boolean = false

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_gallery_item, parent, false)
        return PhotoViewHolder(itemView)
    }

    override fun getItemCount() = items.count()

    // TODO: you should NEVER use hashcode as an item ID — clashes are too likely and it's very risky to do.
    // You could get crashes (stableIDs must be unique)! In this case we're ok with it because there's a low
    // amount of items, and all of them have UUIDs, which makes the risk of clashes low (but not zero)
    override fun getItemId(position: Int) = items[position].item.id.hashCode().toLong()

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val item = items[position]
        loadImage(item.item.imageUrl, holder.imageView)

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

    private fun onItemClicked(photo: Photo) {
        navController.navigate(ListFragmentDirections.actionListFragmentToDetailFragment(photo.id.rawId))
    }

    private fun onItemLongClicked(photo: Photo, selected: Boolean) {
        val changedIndex = items.indexOfFirst { it.item == photo }
        if (changedIndex < 0) {
            Timber.e("Item $photo has been long pressed but I cannot find it!")
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

    fun replaceItemsWith(items: List<Photo>) {
        this.items = items.map { Selectable(it) }
        notifyDataSetChanged() // TODO use diffutils
    }

    fun cancelMultiselect() {
        items.filter { it.selected }
            .forEach { onItemLongClicked(it.item, false) }
    }

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imageView = view.findViewById<ImageView>(R.id.itemImage)!!
        val checkImageView = view.findViewById<ImageView>(R.id.itemCheckedImage)!!
    }

    private data class Selectable<T>(val item: T, val selected: Boolean = false)

    private data class WhatChanged(val selected: Boolean = false)
}
