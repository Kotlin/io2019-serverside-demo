package com.jetbrains.iogallery.support

import android.content.Context
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageOptions
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import okio.Okio
import java.io.ByteArrayInputStream

fun Context.picasso(): Picasso {
    return Picasso.Builder(this)
        .addRequestHandler(GoogleCloudStorageRequestHandler)
        .build()
}

private object GoogleCloudStorageRequestHandler : RequestHandler() {

    override fun canHandleRequest(data: Request): Boolean = data.uri.scheme == GOOGLE_STORAGE_SCHEME

    override fun load(request: Request, networkPolicy: Int): Result {
        val storage = StorageOptions.getDefaultInstance().service
        val blob = storage.get(BlobId.of("cloud-kotlin-io19", request.uri.path))

        val imageBytes = blob.getContent()
        return Result(Okio.source(ByteArrayInputStream(imageBytes)), Picasso.LoadedFrom.NETWORK)
    }

    private const val GOOGLE_STORAGE_SCHEME = "gs"
}
