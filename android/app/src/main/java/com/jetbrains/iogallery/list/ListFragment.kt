package com.jetbrains.iogallery.list

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
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.jetbrains.iogallery.ImagesViewModel
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.list.batch.BatchOperationDialogFragment
import com.jetbrains.iogallery.list.batch.BatchOperationType
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.Photos
import com.jetbrains.iogallery.support.MarginItemDecoraton
import com.jetbrains.iogallery.support.PrimaryActionModeCallback
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber

class ListFragment : Fragment() {

    private lateinit var viewModel: ImagesViewModel

    private val actionMode = PrimaryActionModeCallback()
    private var selectedItems: List<Photo> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_list, container, false)

    private lateinit var photosAdapter: PhotosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyText2.setOnClickListener { onAddImagesClicked() }
        fab.setOnClickListener { onAddImagesClicked() }

        val navController = findNavController()
        photosAdapter = PhotosAdapter(requireActivity(), navController, ::onItemMultiselectChanged)
        actionMode.onActionItemClickListener = ::onActionModeItemClicked
        actionMode.onActionModeFinishedListener = ::finishActionMode

        recyclerView.layoutManager = GridLayoutManager(view.context, 2).apply { orientation = VERTICAL }
        recyclerView.adapter = photosAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(MarginItemDecoraton(view.resources.getDimensionPixelSize(R.dimen.grid_images_margin)))

        viewModel = ViewModelProviders.of(this).get(ImagesViewModel::class.java)
        viewModel.imageEntries.observe(this, Observer(::onImagesListChanged))

        setHasOptionsMenu(true)

        loadImages(freshLoading = true)
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
            R.id.menu_b_and_w -> startBatchOperation(BatchOperationType.BLACK_AND_WHITE)
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
        }
        viewModel.fetchImageEntries()
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

        actionMode.startActionMode(requireActivity(), R.menu.details)
        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .duration = 200
    }

    private fun finishActionMode() {
        if (!actionMode.isActive) return

        actionMode.finishActionMode()
        selectedItems = emptyList()
        photosAdapter.cancelMultiselect()

        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .duration = 200
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
        val directions = ListFragmentDirections.actionListFragmentToUploadFragment(imageUris.toTypedArray())
        findNavController().navigate(directions)
    }

    private fun onImagesListChanged(photos: Photos) {
        progressBar.isVisible = false
        fab.isVisible = true
        if (photos.isEmpty) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
            photosAdapter.replaceItemsWith(photos.photos)
        }
    }

    private fun showEmptyState(visible: Boolean) {
        emptyState.isVisible = visible
        recyclerView.isVisible = !visible
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_refresh) {
            loadImages(freshLoading = false)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

private const val REQUEST_ADD_IMAGES: Int = 8762
