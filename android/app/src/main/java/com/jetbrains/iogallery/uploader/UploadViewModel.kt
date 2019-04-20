package com.jetbrains.iogallery.uploader

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.PhotosCrudBackend
import com.jetbrains.iogallery.api.UploadException
import com.jetbrains.iogallery.api.crudRetrofit
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class UploadViewModel : ViewModel() {

    private val backend by lazy { crudRetrofit() }

    fun uploadImages(contentResolver: ContentResolver, vararg imageUris: Uri): LiveData<UploadEvent> {
        val liveData = MediatorLiveData<UploadEvent>()
        liveData.value = UploadEvent.Started

        Thread().run {
            val totalCount = imageUris.size
            val successesCount = AtomicInteger(0)
            val failuresCount = AtomicInteger(0)

            fun remainingCount() = totalCount - successesCount.get() - failuresCount.get()

            imageUris.forEach { uri ->
                val imageLiveData = backend.uploadImage(contentResolver, uri)
                liveData.addSource(imageLiveData) {
                    if (it.isSuccess) {
                        Timber.i("Image $uri uploaded")
                        liveData.postValue(UploadEvent.ImageUploadSuccess(uri, remainingCount()))
                        successesCount.incrementAndGet()
                    } else {
                        val message = it.exceptionOrNull()?.message ?: "Unknown error"
                        Timber.w("Image uploading failed for $uri: $message")
                        liveData.postValue(UploadEvent.ImageUploadFailure(uri, UploadException(message), remainingCount()))
                        failuresCount.incrementAndGet()
                    }

                    Timber.d("Remaining images: ${remainingCount()}")
                    if (remainingCount() == 0) {
                        liveData.postValue(UploadEvent.Finished(successesCount.get(), failuresCount.get()))
                    }
                }
            }
        }

        return liveData
    }

    private fun PhotosCrudBackend.uploadImage(contentResolver: ContentResolver, uri: Uri): LiveData<Result<Unit>> {
        val inputStream = contentResolver.openInputStream(uri)
            ?: return MutableLiveData<Result<Unit>>().also {
                it.postValue(Result.failure(UploadException("Unable to access content at $uri")))
            }

        val resolvedType = contentResolver.getType(uri)
        val mediaType = resolvedType?.let { MediaType.get(resolvedType) } ?: MediaType.get("image/*")
        val imageBody = RequestBody.create(mediaType, inputStream.readBytes())
        val fileName = uri.getFileName(contentResolver) ?: "no-image-name"

        return uploadPhoto(
            photo = MultipartBody.Part.createFormData("file", fileName, imageBody)
        )
    }

    private fun Uri.getFileName(contentResolver: ContentResolver): String? {
        when (scheme!!.toLowerCase(Locale.US)) {
            "content" -> {
                contentResolver.query(this, null, null, null, null).use {
                    val cursor = it ?: return null
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
            "file" -> return this.toFile().name.let { name ->
                if (name.isEmpty()) null else name
            }
        }
        return null
    }
}

sealed class UploadEvent {

    object Started : UploadEvent()

    data class Finished(val uploaded: Int, val failed: Int) : UploadEvent()

    data class ImageUploadSuccess(val uri: Uri, val remainingCount: Int) : UploadEvent()

    data class ImageUploadFailure(val uri: Uri, val error: Throwable, val remainingCount: Int) : UploadEvent()
}
