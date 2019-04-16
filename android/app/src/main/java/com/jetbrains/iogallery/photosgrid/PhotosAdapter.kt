package com.jetbrains.iogallery.photosgrid

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.model.Photo
import com.squareup.picasso.Picasso
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

        holder.isSelected = item.selected
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val whatChanged = payloads.firstOrNull { it is WhatChanged } as WhatChanged?
            if (whatChanged == null) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                holder.isSelected = whatChanged.selected
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun loadImage(imageUrl: String, targetView: ImageView) {
        Picasso.get()
            .load(imageUrl.toUri())
            .fit()
            .centerCrop()
            .placeholder(R.drawable.loading_placeholder)
            .error(R.drawable.broken_placeholder)
            .into(targetView)
    }

    private fun onItemClicked(photo: Photo) {
        navController.navigate(PhotosGridFragmentDirections.actionPhotosGridFragmentToDetailFragment(photo.id.rawId))
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

        notifyItemChanged(changedIndex, WhatChanged(selected = selected))

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

        var isSelected: Boolean = false
            set(value) {
                field = value
                itemView.isSelected = value

                setSelectedUiState(value)
            }

        private fun setSelectedUiState(selected: Boolean) {
            if (itemView.isAttachedToWindow) {
                imageView.animate()
                    .setInterpolator(FastOutSlowInInterpolator())
                    .scaleX(if (selected) .9f else 1f)
                    .scaleY(if (selected) .9f else 1f)
                    .duration = SELECTION_ANIMATION_DURATION_MS

                checkImageView.animate()
                    .alpha(if (selected) 1f else 0f)
                    .duration = SELECTION_ANIMATION_DURATION_MS
            } else {
                imageView.scaleX = if (selected) .9f else 1f
                imageView.scaleY = if (selected) .9f else 1f
                checkImageView.alpha = if (selected) 1f else 0f
            }
        }

        val imageView = view.findViewById<ImageView>(R.id.itemImage)!!

        private val checkImageView = view.findViewById<ImageView>(R.id.itemCheckedImage)!!
    }

    private data class Selectable<T>(val item: T, val selected: Boolean = false)

    private data class WhatChanged(val selected: Boolean = false)

    companion object {
        const val SELECTION_ANIMATION_DURATION_MS = 100L
    }
}
