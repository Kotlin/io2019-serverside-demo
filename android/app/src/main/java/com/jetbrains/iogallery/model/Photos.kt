package com.jetbrains.iogallery.model

data class Photos(val photos: List<Photo>) {

    operator fun get(photoId: PhotoId) = photos.first { it.id == photoId }

    val isEmpty = photos.isEmpty()
}

data class Photo(val id: PhotoId, val imageUrl: String, val label: String? = null)

data class PhotoId(val rawId: String) {

    override fun toString() = rawId
}
