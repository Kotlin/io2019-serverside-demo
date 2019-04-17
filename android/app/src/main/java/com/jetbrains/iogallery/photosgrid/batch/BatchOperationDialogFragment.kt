package com.jetbrains.iogallery.photosgrid.batch

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
import com.jetbrains.iogallery.model.PhotoId
import com.jetbrains.iogallery.support.requiredPhotoIdsFromRawStringArray
import com.jetbrains.iogallery.support.requiredSerializableArgument
import kotlinx.android.synthetic.main.fragment_batch.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class BatchOperationDialogFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ImagesViewModel

    private val ids by requiredPhotoIdsFromRawStringArray(ARG_IDS)
    private val operationType: BatchOperationType by requiredSerializableArgument(ARG_OPERATION_TYPE)
    private val pendingOperations = AtomicInteger()
    private val failedIds = mutableListOf<PhotoId>()

    var onDismissListener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_batch, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ImagesViewModel::class.java)

        pendingOperations.set(ids.count())
        when (operationType) {
            BatchOperationType.DELETE -> askConfirmationForBatchDelete()
            BatchOperationType.BLACK_AND_WHITE -> makeImagesBlackAndWhite()
        }
        cancelButton.setOnClickListener { dismiss() }
    }

    private fun askConfirmationForBatchDelete() {
        confirmButton.setOnClickListener { onDeleteConfirmed() }
        dialogTitle.setText(R.string.delete_confirmation_title)
        dialogMessage.text = resources.getQuantityString(R.plurals.delete_confirmation_blurb, ids.size)
    }

    private fun onDeleteConfirmed() {
        isCancelable = false
        requireView().isEnabled = false
        hideDialogMessageViews()
        progressBar.isInvisible = false

        ids.forEach { id ->
            Timber.d("Deleting image $id...")
            viewModel.deleteImage(id)
                .observe(this, Observer { result ->
                    if (result.isSuccess) {
                        onImageDeleted(id)
                    } else {
                        onDeleteFailure(id, result)
                    }
                })
        }
    }

    private fun onImageDeleted(id: PhotoId) {
        Timber.i("Image $id deleted successfully.")
        onImageProcessed()
    }

    private fun onDeleteFailure(id: PhotoId, result: Result<Unit>) {
        Timber.e(result.exceptionOrNull()!!, "Ruh roh! Error while deleting image $id")
        failedIds += id
        onImageProcessed()
    }

    private fun makeImagesBlackAndWhite() {
        isCancelable = false
        requireView().isEnabled = false
        hideDialogMessageViews()
        progressBar.isInvisible = false

        ids.forEach { id ->
            Timber.d("Converting image $id to B&W...")
            viewModel.makeImageBlackAndWhite(id)
                .observe(this, Observer { result ->
                    if (result.isSuccess) {
                        onImageConverted(id)
                    } else {
                        onConversionFailure(id, result)
                    }
                })
        }
    }

    private fun hideDialogMessageViews() {
        dialogTitle.isInvisible = true
        dialogMessage.isInvisible = true
        confirmButton.isInvisible = true
        cancelButton.isInvisible = true
        dialogImage.isInvisible = true
    }

    private fun onImageConverted(id: PhotoId) {
        Timber.i("Image $id converted successfully.")
        onImageProcessed()
    }

    private fun onConversionFailure(id: PhotoId, result: Result<Unit>) {
        Timber.e(result.exceptionOrNull()!!, "Ruh roh! Error while converting image $id")
        failedIds += id
        onImageProcessed()
    }

    private fun onImageProcessed() {
        val remainingOperationsCount = pendingOperations.decrementAndGet()
        if (remainingOperationsCount == 0) showCompletionUI()
    }

    private fun showCompletionUI() {
        Timber.i("All done! Batch operation ${operationType.name} completed. Failed ${failedIds.size} out of ${ids.size}")
        progressBar.isInvisible = true

        val hadFailures = failedIds.isNotEmpty()
        if (hadFailures) {
            dialogResultImage.setImageResource(R.drawable.ic_error)
            dialogResultMessage.text = getErrorText(failedIds.count())
        } else {
            dialogResultImage.setImageResource(R.drawable.ic_check)
            dialogResultMessage.text = getSuccessText(ids.size)
        }
        dialogResultGroup.isVisible = true

        findNavController().popBackStack(R.id.photosGridFragment, false)
        dismissAfterDelay()
    }

    private fun getErrorText(failedCount: Int) = when (operationType) {
        BatchOperationType.DELETE -> resources.getQuantityString(R.plurals.delete_result_blurb_error, failedCount, failedCount)
        BatchOperationType.BLACK_AND_WHITE -> resources.getQuantityString(R.plurals.b_and_w_result_blurb_error, failedCount)
    }

    private fun getSuccessText(imagesCount: Int) = when (operationType) {
        BatchOperationType.DELETE -> resources.getQuantityString(R.plurals.delete_result_blurb_success, imagesCount, imagesCount)
        BatchOperationType.BLACK_AND_WHITE -> resources.getQuantityString(R.plurals.b_and_w_result_blurb_success, imagesCount)
    }

    private fun dismissAfterDelay() {
        dialogResultGroup.postDelayed(1500) { dismiss() }
    }

    override fun dismiss() {
        super.dismiss()
        onDismissListener?.invoke()
    }

    companion object {
        val ARG_IDS = "${this::class.java.name}.ARG_IDS"
        val ARG_OPERATION_TYPE = "${this::class.java.name}.ARG_OPERATION_TYPE"
    }
}
