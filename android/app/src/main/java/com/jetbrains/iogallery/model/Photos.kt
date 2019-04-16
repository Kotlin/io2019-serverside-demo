package com.jetbrains.iogallery.model

data class Photos(val photos: List<Photo>) {

    operator fun get(photoId: PhotoId) = photos.first { it.id == photoId }

    val isEmpty = photos.isEmpty()
}

data class Photo(val id: PhotoId, val label: String? = null) {

    val imageUrl = "https://cloud-kotlin-io19.appspot.com/image/$id"
}

data class PhotoId(val rawId: String) {

    override fun toString() = rawId
}
