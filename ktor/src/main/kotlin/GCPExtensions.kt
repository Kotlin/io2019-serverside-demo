package com.jetbrains.ktorServer

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import io.ktor.application.ApplicationCall

sealed class StorageResult {
    class LoadSuccess(val data: ByteArray): StorageResult()
    class SaveSucess: StorageResult()
    class Failure(val message: String): StorageResult()
}


fun ApplicationCall.loadFromStorage(bucket: String, id: String): StorageResult {
    return try {
        val storage = StorageOptions.getDefaultInstance().service
        val blob = BlobId.of(bucket, id)
        StorageResult.LoadSuccess(storage.readAllBytes(blob))
    } catch (e: StorageException) {
        StorageResult.Failure(e.message ?: "Something went wrong")
    }
}

fun ApplicationCall.saveToStorage(bucket: String, id: String, blob: ByteArray): StorageResult {
    return try {
        val blobMono = BlobId.of(bucket, id)
        val blobInfo = BlobInfo.newBuilder(blobMono).setContentType("application/octet-stream").build()
        val storage = StorageOptions.getDefaultInstance().service
        storage.create(blobInfo, blob)
        StorageResult.SaveSucess()
    } catch (e: StorageException) {
        StorageResult.Failure(e.message ?: "Something went wrong")
    }
}