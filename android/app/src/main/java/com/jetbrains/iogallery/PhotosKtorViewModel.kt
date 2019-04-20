package com.jetbrains.iogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.jetbrains.iogallery.api.ktorRetrofit
import com.jetbrains.iogallery.model.PhotoId

class PhotosKtorViewModel : ViewModel() {

    private val backend by lazy { ktorRetrofit() }

    fun makeImageMonochrome(id: PhotoId): LiveData<Result<Unit>> = backend.makeImageMonochrome(id.rawId)
}
