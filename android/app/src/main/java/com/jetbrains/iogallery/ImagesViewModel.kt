package com.jetbrains.iogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.api.ImagesBackend
import com.jetbrains.iogallery.api.retrofit
import com.jetbrains.iogallery.model.ApiPhotos
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.PhotoId
import com.jetbrains.iogallery.model.Photos
import com.shopify.livedataktx.filter
import com.shopify.livedataktx.map
import com.shopify.livedataktx.toKtx
import timber.log.Timber

class ImagesViewModel(private val apiServerProvider: () -> ApiServer) : ViewModel() {

    private val backend
        get() = retrofit(apiServerProvider()).create(ImagesBackend::class.java)

    private val imageEntriesMediator = MediatorLiveData<ApiPhotos>()

    private var currentImageEntriesSource: LiveData<ApiPhotos> = MutableLiveData<ApiPhotos>().also {
        it.value = ApiPhotos.EMPTY // Initial value
    }

    val imageEntries = imageEntriesMediator.toKtx()
        .map { apiPhotos ->
            Photos(
                apiPhotos.embedded.photos.map { apiPhoto ->
                    Photo(PhotoId(apiPhoto.rawId))
                }
            )
        }

    fun fetchImageEntries() {
        Timber.i("Fetching images list...")
        imageEntriesMediator.removeSource(currentImageEntriesSource)

        // TODO: handle in-flight and error states properly
        currentImageEntriesSource = backend.fetchPhotosList().toKtx()
            .filter { it.isSuccess }
            .map { it.getOrThrow() }

        imageEntriesMediator.addSource(currentImageEntriesSource) { imageEntriesMediator.postValue(it) }
    }

    fun categorizeImage(id: PhotoId): LiveData<Result<Unit>> = backend.categorizeImage(id.rawId)

    fun makeImageBlackAndWhite(id: PhotoId): LiveData<Result<Unit>> = backend.makeImageBlackAndWhite(id.rawId)

    fun deleteImage(id: PhotoId): LiveData<Result<Unit>> = backend.deleteImage(id.rawId)
}
