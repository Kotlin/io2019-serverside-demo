package com.jetbrains.iogallery.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jetbrains.iogallery.ImagesViewModel
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.debug.DebugPreferences
import com.jetbrains.iogallery.support.requiredPhotoIdFromRawString
import com.jetbrains.iogallery.support.viewModelFactory
import kotlinx.android.synthetic.main.fragment_deletion_confirmation.*
import timber.log.Timber

class DeletionConfirmationDialogFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ImagesViewModel
    private lateinit var debugPreferences: DebugPreferences

    private var currentApiServer: ApiServer = ApiServer.SWAGGER
    private val id by requiredPhotoIdFromRawString(ARG_ID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_deletion_confirmation, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugPreferences = DebugPreferences(requireActivity())
        debugPreferences.subscribeToApiServer(this) { apiServer -> currentApiServer = apiServer }

        viewModel = ViewModelProviders.of(this, viewModelFactory { ImagesViewModel { currentApiServer } })
            .get(ImagesViewModel::class.java)
        dialogMessage.text = resources.getQuantityString(R.plurals.delete_confirmation_blurb, 1)

        deleteButton.setOnClickListener { onDeleteConfirmed() }
        cancelButton.setOnClickListener { dismiss() }
    }

    @SuppressLint("Range") // This is a bug in the Lint check — it's flagging the default value of args.id
    private fun onDeleteConfirmed() {
        isCancelable = false
        requireView().isEnabled = false
        dialogConfirmationGroup.isInvisible = true
        progressBar.isInvisible = false

        viewModel.deleteImage(id)
            .observe(this, Observer { result ->
                progressBar.isInvisible = true
                if (result.isSuccess) {
                    onImageDeleted()
                } else {
                    onDeleteFailure(result)
                }
            })
    }

    private fun onImageDeleted() {
        Timber.i("Image $id deleted successfully, going back to list.")

        dialogResultGroup.isVisible = true
        dialogResultImage.setImageResource(R.drawable.ic_check)
        dialogResultMessage.text = resources.getQuantityString(R.plurals.delete_result_blurb_success, 1)

        findNavController().popBackStack(R.id.listFragment, false)
        dismissAfterDelay()
    }

    private fun onDeleteFailure(result: Result<Unit>) {
        Timber.e(result.exceptionOrNull()!!, "Ruh roh! Error while deleting image $id")

        dialogResultGroup.isVisible = true
        dialogResultImage.setImageResource(R.drawable.ic_error)
        dialogResultMessage.text = resources.getQuantityString(R.plurals.delete_result_blurb_error, 1)

        dismissAfterDelay()
    }

    private fun dismissAfterDelay() {
        dialogResultGroup.postDelayed(1500) { dismiss() }
    }

    companion object {
        val ARG_ID = "${this::class.java.name}.ARG_ID"
    }
}