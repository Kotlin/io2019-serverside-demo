package com.jetbrains.iogallery.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.jetbrains.iogallery.ImagesViewModel
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.debug.DebugPreferences
import com.jetbrains.iogallery.model.ImageEntry
import com.jetbrains.iogallery.support.picasso
import com.jetbrains.iogallery.support.viewModelFactory
import com.squareup.picasso.Callback
import kotlinx.android.synthetic.main.fragment_details.*
import timber.log.Timber

class DetailFragment : Fragment() {

    private lateinit var viewModel: ImagesViewModel
    private lateinit var debugPreferences: DebugPreferences

    private var currentApiServer: ApiServer = ApiServer.SWAGGER
    private val args: DetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_details, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomToolbar.replaceMenu(R.menu.details)
        bottomToolbar.setOnMenuItemClickListener { onBottomToolbarMenuItemClicked(it) }

        debugPreferences = DebugPreferences(requireActivity())
        debugPreferences.subscribeToApiServer(this) { apiServer -> currentApiServer = apiServer }

        viewModel = ViewModelProviders.of(this, viewModelFactory { ImagesViewModel { currentApiServer } })
            .get(ImagesViewModel::class.java)

        viewModel.imageEntries.observe(this, Observer(::onImagesListChanged))
        loadImageData()

        fab.setOnClickListener { onCategorizeImageClicked() }
    }

    @SuppressLint("Range") // This is a bug in the Lint check — it's flagging the default value of args.id
    private fun onCategorizeImageClicked() {
        detailImage.animate().alpha(.75f)
        viewModel.categorizeImage(args.id)
            .observe(this, Observer { result ->
                if (result.isSuccess) {
                    Timber.i("Image ${args.id} categorised successfully, reloading data.")
                    loadImageData()
                } else {
                    detailImage.animate().alpha(1f)
                    val errorMessage = "Error while categorising image ${args.id}"
                    Timber.e(result.exceptionOrNull()!!, "Ruh roh! $errorMessage")
                    Snackbar.make(detailsRoot, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    private fun loadImageData() {
        Timber.d("Loading images list...")
        progressBar.isVisible = true
        viewModel.fetchImageEntries()
    }

    private fun onImagesListChanged(list: List<ImageEntry>) {
        val imageEntry = list.first { it.id == args.id }

        val shouldAnimate = !imageLabel.isVisible
        updateImageLabelView(imageEntry, shouldAnimate)

        val imageUrl = imageEntry.url
        loadImage(imageUrl)
    }

    private fun updateImageLabelView(imageEntry: ImageEntry, shouldAnimate: Boolean) {
        if (imageEntry.label != null) {
            imageLabel.text = imageEntry.label
            imageLabel.isVisible = true

            if (shouldAnimate) {
                imageLabel.translationY = resources.getDimensionPixelSize(R.dimen.label_translation_y).toFloat()
                imageLabel.alpha = 0f
                imageLabel.animate().alpha(1f).translationY(0f)
            }
        } else {
            imageLabel.isVisible = false
        }
    }

    private fun loadImage(imageUrl: String) {
        Timber.d("Loading image $imageUrl...")
        detailImage.context.picasso()
            .load("$imageUrl?fm=webp".toUri()) // TODO this should be changed to a call to /image/{id}
            .fit()
            .centerInside()
            .error(R.drawable.broken_placeholder)
            .into(detailImage, object : Callback {
                override fun onError(e: Exception?) {
                    progressBar.isVisible = false
                    Timber.e(e, "Error loading: $imageUrl")
                    Snackbar.make(detailsRoot, "Error loading image", Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }

                override fun onSuccess() {
                    detailImage.animate().alpha(1f)
                    progressBar.isVisible = false
                    Timber.i("Image loaded: $imageUrl")
                }
            })
    }

    private fun onBottomToolbarMenuItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> onDeleteClicked(args.id)
            R.id.menu_b_and_w -> onBlackAndWhiteClicked(args.id)
            else -> return false
        }
        return true
    }

    private fun onDeleteClicked(id: Long) {
        val dialog = DeletionConfirmationDialogFragment()
        dialog.arguments = Bundle().also { it.putLong(DeletionConfirmationDialogFragment.ARG_ID, id) }
        dialog.showNow(requireActivity().supportFragmentManager, "CONFIRMATION")
    }

    private fun onBlackAndWhiteClicked(id: Long) {
        detailImage.animate().alpha(.75f)
        viewModel.makeImageBlackAndWhite(id)
            .observe(this, Observer { result ->
                if (result.isSuccess) {
                    Timber.i("Image ${args.id} desaturated successfully, reloading data.")
                    Snackbar.make(detailsRoot, "Image successfully converted to black & white", Snackbar.LENGTH_SHORT).show()
                    loadImageData()
                } else {
                    detailImage.animate().alpha(1f)
                    val errorMessage = "Error while hipstering image"
                    Timber.e(result.exceptionOrNull()!!, "Ruh roh! $errorMessage ${args.id}")
                    Snackbar.make(detailsRoot, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            })
    }
}
