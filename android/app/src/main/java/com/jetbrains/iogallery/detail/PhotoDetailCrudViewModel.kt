package com.jetbrains.iogallery.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.Endpoint
import com.jetbrains.iogallery.api.crudRetrofit
import com.jetbrains.iogallery.model.ApiPhotos.ApiEmbedded.ApiPhoto
import com.jetbrains.iogallery.model.Photo
import com.jetbrains.iogallery.model.PhotoId
import com.shopify.livedataktx.map
import com.shopify.livedataktx.toKtx
import timber.log.Timber

class PhotoDetailCrudViewModel(private val id: PhotoId) : ViewModel() {

    private val backend by lazy { crudRetrofit() }

    private val photoMediator = MediatorLiveData<Result<ApiPhoto>>()

    private var photoSource: LiveData<Result<ApiPhoto>?> = MutableLiveData<Result<ApiPhoto>>().also {
        it.value = null // Initial value
    }

    val photo = photoMediator.toKtx()
        .map { result ->
            if (result.isSuccess) {
                val apiPhoto = result.getOrThrow()
                val imageUrl = Endpoint.CRUD.baseUrl + apiPhoto.uri
                val label = apiPhoto.label?.substringBefore(',')
                Photo(PhotoId(apiPhoto.rawId), imageUrl, label)
            } else {
                null
            }
        }

    fun fetchPhoto() {
        Timber.i("Fetching photo data...")
        photoMediator.removeSource(photoSource)

        // TODO: handle in-flight and error states properly
        photoSource = backend.photo(id.rawId) as LiveData<Result<ApiPhoto>?>

        photoMediator.addSource(photoSource) {
            photoMediator.postValue(it)
        }
    }
}
