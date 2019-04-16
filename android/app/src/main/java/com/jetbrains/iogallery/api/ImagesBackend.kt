package com.jetbrains.iogallery.api

import androidx.annotation.IntRange
import androidx.lifecycle.LiveData
import com.jetbrains.iogallery.model.ImageEntry
import com.jetbrains.iogallery.model.UploadResult
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ImagesBackend {

    @GET("photos")
    fun fetchImagesList(): LiveData<Result<List<ImageEntry>>>

    @Multipart
    @POST("upload")
    fun uploadImage(@Part imageFile: MultipartBody.Part): LiveData<Result<UploadResult>>

    @DELETE("image/{id}")
    fun deleteImage(@Path("id") @IntRange(from = 0L) id: Long): LiveData<Result<Unit>>

    @PUT("image/{id}/categorize")
    fun categorizeImage(@Path("id") @IntRange(from = 0L) id: Long): LiveData<Result<Unit>>

    @PUT("image/{id}/b-and-w")
    fun makeImageBlackAndWhite(@Path("id") @IntRange(from = 0L) id: Long): LiveData<Result<Unit>>
}
