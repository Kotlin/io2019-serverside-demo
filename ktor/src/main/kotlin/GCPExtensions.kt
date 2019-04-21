package com.jetbrains.ktorServer

import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.StructuredQuery
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.jetbrains.ktorServer.routes.Photo
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


fun queryDataStore(unprocessed: Boolean = false): List<Photo> {
    val dataStore = DatastoreOptions.getDefaultInstance().service
    val queryBuilder = StructuredQuery.newEntityQueryBuilder()
        .setKind("photo")
    if (unprocessed) {
        queryBuilder.setFilter(StructuredQuery.PropertyFilter.isNull("label"))
    }
    val query = queryBuilder.build()
    val resultSet = dataStore.run(query)
    val results = resultSet.asSequence().map {
        Photo(it.getString("id"), it.getString("label") ?: "", it.getString("uri"))
    }
    return results.toList()
}


