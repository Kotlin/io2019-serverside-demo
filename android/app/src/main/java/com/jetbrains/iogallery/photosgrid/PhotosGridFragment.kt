package com.jetbrains.iogallery.photosgrid

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.jetbrains.iogallery.MainActivity
import com.jetbrains.iogallery.PhotosCrudViewModel
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.Photos
import com.jetbrains.iogallery.photosgrid.batch.BatchOperationDialogFragment
import com.jetbrains.iogallery.photosgrid.batch.BatchOperationType
import com.jetbrains.iogallery.support.MarginItemDecoraton
import com.jetbrains.iogallery.support.PrimaryActionModeCallback
import kotlinx.android.synthetic.main.fragment_photos_grid.*
import timber.log.Timber

class PhotosGridFragment : Fragment() {

    private lateinit var viewModel: PhotosCrudViewModel

    private val actionMode = PrimaryActionModeCallback()
    private var selectedItems: List<Photo> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_photos_grid, container, false)

    private lateinit var photosAdapter: PhotosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyText2.setOnClickListener { onAddImagesClicked() }
        fab.setOnClickListener { onAddImagesClicked() }

        photosAdapter = PhotosAdapter(requireActivity(), ::onItemClicked, ::onItemMultiselectChanged)
        actionMode.onActionItemClickListener = ::onActionModeItemClicked
        actionMode.onActionModeFinishedListener = ::finishActionMode

        recyclerView.layoutManager = GridLayoutManager(view.context, 2).apply { orientation = VERTICAL }
        recyclerView.adapter = photosAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(MarginItemDecoraton(view.resources.getDimensionPixelSize(R.dimen.grid_images_margin)))

        viewModel = ViewModelProviders.of(this).get(PhotosCrudViewModel::class.java)
        viewModel.photos.observe(this, Observer(::onImagesListChanged))

        setHasOptionsMenu(true)

        loadImages(freshLoading = true)
    }

    private fun onItemClicked(photo: Photo) {
        fab.animate()
            .setInterpolator(FastOutLinearInInterpolator())
            .translationY(fab.height / 2F)
            .duration = 150

        findNavController()
            .navigate(PhotosGridFragmentDirections.actionPhotosGridFragmentToDetailFragment(photo.id.rawId))
    }

    private fun onAddImagesClicked() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val photosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, photosDir.toUri())
            }
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_ADD_IMAGES)
    }

    private fun onActionModeItemClicked(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_delete -> startBatchOperation(BatchOperationType.DELETE)
            R.id.menu_b_and_w -> startBatchOperation(BatchOperationType.MONOCHROME)
        }
    }

    private fun startBatchOperation(operation: BatchOperationType) {
        val dialog = BatchOperationDialogFragment()
        val selectedIds = selectedItems.map { it.id.rawId }.toTypedArray()

        dialog.arguments = Bundle().also {
            it.putStringArray(BatchOperationDialogFragment.ARG_IDS, selectedIds)
            it.putSerializable(BatchOperationDialogFragment.ARG_OPERATION_TYPE, operation)
        }
        val supportFragmentManager = requireActivity().supportFragmentManager
        dialog.showNow(supportFragmentManager, "BATCH")
        dialog.onDismissListener = { loadImages(freshLoading = false) }
    }

    private fun loadImages(freshLoading: Boolean) {
        if (freshLoading) {
            fab.isVisible = false
            recyclerView.isVisible = false
            emptyState.isVisible = false
            progressBar.isVisible = true
        } else {
            progressBar.isVisible = true
            animateFabScale(0F)
            recyclerView.animate()
                .alpha(.75F)
                .duration = 200
            recyclerView.isEnabled = false
        }
        viewModel.fetchPhotos()
    }

    private fun onItemMultiselectChanged(newSelectedItems: List<Photo>) {
        when {
            newSelectedItems.isEmpty() && selectedItems.isNotEmpty() -> finishActionMode()
            newSelectedItems.isNotEmpty() && selectedItems.isEmpty() -> startActionMode()
            newSelectedItems.isNotEmpty() -> actionMode.selectedItemsCount = newSelectedItems.count()
        }
        selectedItems = newSelectedItems
    }

    private fun startActionMode() {
        if (actionMode.isActive) return

        actionMode.startActionMode(requireActivity(), R.menu.photos_grid_action_mode)
        animateStatusBarColorTo(R.color.primaryLightColor)

        animateFabScale(0F)
    }

    private fun finishActionMode() {
        if (!actionMode.isActive) return

        actionMode.finishActionMode()
        animateStatusBarColorTo(R.color.primaryColor)
        selectedItems = emptyList()
        photosAdapter.cancelMultiselect()

        animateFabScale(1F)
    }

    private fun animateFabScale(@FloatRange(from = 0.0, to = 1.0) scale: Float) {
        fab.animate()
            .scaleX(scale)
            .scaleY(scale)
            .duration = 200
    }

    private fun animateStatusBarColorTo(@ColorRes endColor: Int) {
        val window = requireActivity().window
        ObjectAnimator.ofInt(
            window,
            "statusBarColor",
            window.statusBarColor,
            resources.getColor(endColor, requireContext().theme)
        ).apply {
            setEvaluator(ArgbEvaluatorCompat())
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ADD_IMAGES && resultCode == Activity.RESULT_OK) {
            onImagesPicked(data?.extractImageUris() ?: emptyList())
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun Intent.extractImageUris(): List<Uri> {
        if (data != null) return listOf(data)
        val clipData = clipData ?: return emptyList()

        return (0 until clipData.itemCount)
            .map { clipData[it].uri }
    }

    private operator fun ClipData.get(position: Int): ClipData.Item = getItemAt(position)

    private fun onImagesPicked(imageUris: List<Uri>) {
        Timber.d("Images picked:\n${imageUris.joinToString(separator = "\n")}")
        val directions = PhotosGridFragmentDirections.actionPhotosGridFragmentToUploadFragment(imageUris.toTypedArray())
        findNavController().navigate(directions)
    }

    private fun onImagesListChanged(photos: Photos) {
        progressBar.isVisible = false
        fab.isVisible = true
        if (photos.isEmpty) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
            if (recyclerView.isEnabled.not()) {
                progressBar.isVisible = false
                animateFabScale(1F)
                recyclerView.animate()
                    .alpha(1F)
                    .duration = 200
                recyclerView.isEnabled = true
            }
            photosAdapter.replaceItemsWith(photos.photos)
        }
    }

    private fun showEmptyState(visible: Boolean) {
        emptyState.isVisible = visible
        recyclerView.isVisible = !visible
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.photos_grid, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_refresh) {
            onRefreshSelected()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onRefreshSelected() {
        val mainActivity = requireActivity() as MainActivity
        val menuItemView = mainActivity.menuItemViews().last()

        menuItemView.animate()
            .setInterpolator(FastOutSlowInInterpolator())
            .rotation(360F)
            .withEndAction { menuItemView.rotation = 0F }
            .duration = 250

        loadImages(freshLoading = false)
    }
}

private const val REQUEST_ADD_IMAGES: Int = 8762
