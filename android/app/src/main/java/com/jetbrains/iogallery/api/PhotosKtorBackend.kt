package com.jetbrains.iogallery.api

import androidx.lifecycle.LiveData
import retrofit2.http.PUT
import retrofit2.http.Path

interface PhotosKtorBackend {

    @PUT("mono/{id}")
    fun makeImageMonochrome(@Path("id") id: String): LiveData<Result<Unit>>
}
