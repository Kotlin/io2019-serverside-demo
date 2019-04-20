package com.jetbrains.iogallery.detail

import android.content.Intent
import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.jetbrains.iogallery.PhotosCrudViewModel
import com.jetbrains.iogallery.PhotosKtorViewModel
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.api.Endpoint
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.PhotoId
import com.jetbrains.iogallery.model.Photos
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_details.*
import timber.log.Timber

class DetailFragment : Fragment() {

    private lateinit var crudViewModel: PhotosCrudViewModel

    private val args: DetailFragmentArgs by navArgs()
    private val photoId get() = PhotoId(args.rawId)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_details, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        crudViewModel = ViewModelProviders.of(this).get(PhotosCrudViewModel::class.java)

        crudViewModel.imageEntries.observe(this, Observer(::onImagesListChanged))
        loadImageData()
    }

    private fun loadImageData() {
        Timber.d("Loading images list...")
        progressBar.isVisible = true
        crudViewModel.fetchImageEntries()
    }

    private fun onImagesListChanged(photos: Photos) {
        if (photos.isEmpty) {
            detailImage.setImageResource(R.drawable.ic_error)
            imageLabel.isVisible = false
            Snackbar.make(detailImage, "Could not load the image.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") { loadImageData() }
            return
        }

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
        Picasso.get()
            .load(imageUrl.toUri())
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> onDeleteClicked(photoId)
            R.id.menu_b_and_w -> onMonochromeClicked(photoId)
            R.id.menu_share -> onShareClicked(photoId)
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onDeleteClicked(id: PhotoId) {
        val dialog = DeletionConfirmationDialogFragment()
        dialog.arguments = Bundle().also { it.putString(DeletionConfirmationDialogFragment.ARG_ID, id.rawId) }
        dialog.showNow(requireActivity().supportFragmentManager, "CONFIRMATION")
    }

    private fun onMonochromeClicked(id: PhotoId) {
        detailImage.animate().alpha(.75f)
        progressBar.isVisible = true
        val ktorViewModel = ViewModelProviders.of(this).get(PhotosKtorViewModel::class.java)
        ktorViewModel.makeImageMonochrome(id)
            .observe(this, Observer { result ->
                progressBar.isVisible = false
                if (result.isSuccess) {
                    Timber.i("Image $photoId desaturated successfully, reloading data.")
                    Snackbar.make(detailsRoot, "Image successfully converted to monochrome", Snackbar.LENGTH_SHORT).show()
                    loadImageData()
                } else {
                    detailImage.animate().alpha(1f)
                    val errorMessage = "Error while hipstering image"
                    Timber.e(result.exceptionOrNull()!!, "Ruh roh! $errorMessage $photoId")
                    Snackbar.make(detailsRoot, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    private fun onShareClicked(photoId: PhotoId) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "${Endpoint.KTOR.baseUrl}share/${photoId.rawId}")
            type = "text/plain"
        }
        requireActivity().startActivity(Intent.createChooser(intent, resources.getText(R.string.send_to)))
    }
}
