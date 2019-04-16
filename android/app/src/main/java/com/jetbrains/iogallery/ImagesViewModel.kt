package com.jetbrains.iogallery

import androidx.annotation.IntRange
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.api.ImagesBackend
import com.jetbrains.iogallery.api.retrofit
import com.jetbrains.iogallery.model.ImageEntry
import com.jetbrains.iogallery.support.livedata.map
import timber.log.Timber

class ImagesViewModel(private val apiServerProvider: () -> ApiServer) : ViewModel() {

    private val backend
        get() = retrofit(apiServerProvider()).create(ImagesBackend::class.java)

    private val imageEntriesMediator = MediatorLiveData<List<ImageEntry>>()

    private var currentImageEntriesSource: LiveData<List<ImageEntry>> = MutableLiveData<List<ImageEntry>>().also {
        it.value = emptyList() // Initial value
    }

    val imageEntries
        get() = imageEntriesMediator

    fun fetchImageEntries() {
        Timber.i("Fetching images list...")
        imageEntriesMediator.removeSource(currentImageEntriesSource)

        currentImageEntriesSource = backend.fetchImagesList()
            .map {
                if (it.isSuccess) {
                    it.getOrThrow()
                } else {
                    emptyList()
                }
            }

        imageEntriesMediator.addSource(currentImageEntriesSource) { imageEntriesMediator.postValue(it) }
    }

    fun categorizeImage(@IntRange(from = 0L) id: Long): LiveData<Result<Unit>> = backend.categorizeImage(id)

    fun makeImageBlackAndWhite(@IntRange(from = 0L) id: Long): LiveData<Result<Unit>> = backend.makeImageBlackAndWhite(id)

    fun deleteImage(@IntRange(from = 0L) id: Long): LiveData<Result<Unit>> = backend.deleteImage(id)
}
