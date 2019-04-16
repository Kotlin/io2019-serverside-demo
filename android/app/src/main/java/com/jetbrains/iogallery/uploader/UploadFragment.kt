package com.jetbrains.iogallery.uploader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.debug.DebugPreferences
import com.jetbrains.iogallery.support.viewModelFactory
import com.jetbrains.iogallery.uploader.UploadEvent.Finished
import com.jetbrains.iogallery.uploader.UploadEvent.ImageUploadFailure
import com.jetbrains.iogallery.uploader.UploadEvent.ImageUploadSuccess
import kotlinx.android.synthetic.main.fragment_upload.*

private const val COMPLETION_MESSAGE_DELAY = 1500L

class UploadFragment : Fragment() {

    private lateinit var viewModel: UploadViewModel
    private lateinit var debugPreferences: DebugPreferences
    private var currentApiServer: ApiServer = ApiServer.SWAGGER
    private val args: UploadFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_upload, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        debugPreferences = DebugPreferences(requireActivity())
        debugPreferences.subscribeToApiServer(this) { apiServer -> currentApiServer = apiServer }

        viewModel = ViewModelProviders.of(this, viewModelFactory { UploadViewModel { currentApiServer } })
            .get(UploadViewModel::class.java)

        val contentResolver = requireContext().contentResolver
        viewModel.uploadImages(contentResolver, *args.imageUris)
            .observe(this, Observer(::onImageUploadEvent))
    }

    private fun onImageUploadEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.Started -> {
                val count = args.imageUris.size
                uploadingLabel.text = resources.getQuantityString(R.plurals.uploading_images_with_count, count, count)
            }
            is ImageUploadSuccess -> onImageUploaded(event.remainingCount)
            is ImageUploadFailure -> onImageUploaded(event.remainingCount)
            is Finished -> onImageUploadCompleted(event.failed)
        }
    }

    private fun onImageUploaded(remainingCount: Int) {
        uploadingLabel.text = resources.getQuantityString(R.plurals.uploading_images_with_count, remainingCount, remainingCount)
    }

    private fun onImageUploadCompleted(failed: Int) {
        progressBar.isVisible = false
        if (failed == 0) {
            uploadingLabel.text = getString(R.string.upload_complete)
            cloudUploadImage.setImageResource(R.drawable.ic_check)
        } else {
            uploadingLabel.text = resources.getQuantityString(R.plurals.uploading_failure_with_count, failed, failed)
            cloudUploadImage.setImageResource(R.drawable.ic_error)
        }

        cloudUploadImage.postDelayed(COMPLETION_MESSAGE_DELAY) { findNavController().popBackStack() }
    }
}
