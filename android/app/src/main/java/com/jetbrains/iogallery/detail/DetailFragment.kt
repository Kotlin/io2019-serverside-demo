package com.jetbrains.iogallery.detail

import android.content.Intent
import android.net.Uri
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
import com.jetbrains.iogallery.support.nukePicassoCache
import com.jetbrains.iogallery.support.provideViewModel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_details.*
import timber.log.Timber

class DetailFragment : Fragment() {

    private lateinit var photosCrudViewModel: PhotosCrudViewModel
    private lateinit var photoDetailCrudViewModel: PhotoDetailCrudViewModel

    private val args: DetailFragmentArgs by navArgs()
    private val photoId get() = PhotoId(args.rawId)
    private var pendingMonochrome: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_details, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        photosCrudViewModel = provideViewModel()
        photoDetailCrudViewModel = provideViewModel { PhotoDetailCrudViewModel(photoId) }

        photoDetailCrudViewModel.photo.observe(this, Observer(::onPhotoDataChanged))
        loadPhotoData()
    }

    private fun onPhotoDataChanged(photo: Photo?) {
        if (photo == null) {
            detailImage.setImageResource(R.drawable.ic_error)
            imageLabel.isVisible = false
            Snackbar.make(detailImage, "Could not load the photo.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") { loadPhotoData() }
            return
        }

        val shouldAnimateLabel = !imageLabel.isVisible
        updateImageLabelView(photo, shouldAnimateLabel)

        loadImage(photo.imageUrl.toUri())
    }

    private fun updateImageLabelView(photo: Photo, shouldAnimate: Boolean) {
        if (photo.label == null) {
            imageLabel.isVisible = false
            return
        }

        imageLabel.text = photo.label
        imageLabel.isVisible = true

        if (shouldAnimate) {
            imageLabel.translationY = resources.getDimensionPixelSize(R.dimen.label_translation_y).toFloat()
            imageLabel.alpha = 0f
            imageLabel.animate().alpha(1f).translationY(0f)
        }
    }

    private fun loadImage(imageUri: Uri) {
        Timber.d("Loading image $imageUri...")
        val picasso = Picasso.get()

        if (pendingMonochrome) {
            nukePicassoCache()
            pendingMonochrome = false
        }

        picasso.load(imageUri)
            .fit()
            .centerInside()
            .error(R.drawable.broken_placeholder)
            .into(detailImage, object : Callback {
                override fun onError(e: Exception?) {
                    progressBar.isVisible = false
                    Timber.e(e, "Error loading: $imageUri")
                    Snackbar.make(detailsRoot, "Error loading image", Snackbar.LENGTH_LONG).show()
                    detailImage.animate().alpha(1f)
                    findNavController().popBackStack()
                }

                override fun onSuccess() {
                    detailImage.animate().alpha(1f)
                    progressBar.isVisible = false
                    invalidateOptionsMenu()
                    Timber.i("Image loaded: $imageUri")
                }
            })
    }

    private fun invalidateOptionsMenu() {
        // This is a silly hack but for whatever reason Fragment doesn't have a real API for this
        setHasOptionsMenu(false)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.details, menu)

        val enabled = detailImage.drawable != null
        listOf(R.id.menu_monochrome, R.id.menu_refresh).forEach { menuId ->
            menu.findItem(menuId).isEnabled = enabled
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> onDeleteClicked(photoId)
            R.id.menu_monochrome -> onMonochromeClicked(photoId)
            R.id.menu_share -> onShareClicked(photoId)
            R.id.menu_refresh -> loadPhotoData()
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
                    pendingMonochrome = true
                    loadPhotoData()
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

    private fun loadPhotoData() {
        Timber.d("Loading photo data...")
        progressBar.isVisible = true
        detailImage.animate().alpha(.75f)
        photoDetailCrudViewModel.fetchPhoto()
    }
}
