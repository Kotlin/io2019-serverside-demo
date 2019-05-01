package com.jetbrains.iogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.Endpoint
import com.jetbrains.iogallery.api.crudRetrofit
import com.jetbrains.iogallery.model.ApiPhotos
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.PhotoId
import com.jetbrains.iogallery.model.Photos
import com.shopify.livedataktx.map
import com.shopify.livedataktx.toKtx
import timber.log.Timber

class PhotosCrudViewModel : ViewModel() {

    private val backend by lazy { crudRetrofit() }

    private val imageEntriesMediator = MediatorLiveData<Result<ApiPhotos>>()

    private var currentImageEntriesSource: LiveData<Result<ApiPhotos>> = MutableLiveData<Result<ApiPhotos>>().also {
        it.value = Result.success(ApiPhotos.EMPTY) // Initial value
    }

    val imageEntries = imageEntriesMediator.toKtx()
        .map { result ->
            if (result.isSuccess) {
                Photos(result.getOrThrow().embedded.photos
                    .map { apiPhoto ->
                        val imageUrl = Endpoint.CRUD.baseUrl + apiPhoto.uri
                        val label = apiPhoto.label.substringBefore(',')
                        Photo(PhotoId(apiPhoto.rawId), imageUrl, label)
                    }
                )
            } else {
                Photos(emptyList())
            }
        }

    fun fetchImageEntries() {
        Timber.i("Fetching images list...")
        imageEntriesMediator.removeSource(currentImageEntriesSource)

        // TODO: handle in-flight and error states properly
        currentImageEntriesSource = backend.fetchPhotosList()

        imageEntriesMediator.addSource(currentImageEntriesSource) {
            imageEntriesMediator.postValue(it)
        }
    }

    fun deleteImage(id: PhotoId): LiveData<Result<Unit>> = backend.deleteImage(id.rawId)
}
