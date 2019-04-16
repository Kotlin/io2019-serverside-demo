package com.jetbrains.iogallery.detail

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
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.PhotoId
import com.jetbrains.iogallery.model.Photos
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
    private val photoId get() = PhotoId(args.rawId)

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

    private fun onCategorizeImageClicked() {
        detailImage.animate().alpha(.75f)
        viewModel.categorizeImage(photoId)
            .observe(this, Observer { result ->
                if (result.isSuccess) {
                    Timber.i("Image $photoId categorised successfully, reloading data.")
                    loadImageData()
                } else {
                    detailImage.animate().alpha(1f)
                    val errorMessage = "Error while categorising image $photoId"
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

    private fun onImagesListChanged(photos: Photos) {
        val photo = photos[photoId]

        val shouldAnimate = !imageLabel.isVisible
        updateImageLabelView(photo, shouldAnimate)

        loadImage(photo.imageUrl)
    }

    private fun updateImageLabelView(photo: Photo, shouldAnimate: Boolean) {
        if (photo.label != null) {
            imageLabel.text = photo.label
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
            R.id.menu_delete -> onDeleteClicked(photoId)
            R.id.menu_b_and_w -> onBlackAndWhiteClicked(photoId)
            else -> return false
        }
        return true
    }

    private fun onDeleteClicked(id: PhotoId) {
        val dialog = DeletionConfirmationDialogFragment()
        dialog.arguments = Bundle().also { it.putString(DeletionConfirmationDialogFragment.ARG_ID, id.rawId) }
        dialog.showNow(requireActivity().supportFragmentManager, "CONFIRMATION")
    }

    private fun onBlackAndWhiteClicked(id: PhotoId) {
        detailImage.animate().alpha(.75f)
        viewModel.makeImageBlackAndWhite(id)
            .observe(this, Observer { result ->
                if (result.isSuccess) {
                    Timber.i("Image $photoId desaturated successfully, reloading data.")
                    Snackbar.make(detailsRoot, "Image successfully converted to black & white", Snackbar.LENGTH_SHORT).show()
                    loadImageData()
                } else {
                    detailImage.animate().alpha(1f)
                    val errorMessage = "Error while hipstering image"
                    Timber.e(result.exceptionOrNull()!!, "Ruh roh! $errorMessage $photoId")
                    Snackbar.make(detailsRoot, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            })
    }
}
