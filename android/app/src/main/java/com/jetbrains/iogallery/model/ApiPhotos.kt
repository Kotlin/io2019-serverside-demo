package com.jetbrains.iogallery.model

import com.google.gson.annotations.SerializedName

data class ApiPhotos(
    @SerializedName("_embedded")
    val embedded: ApiEmbedded,
    @SerializedName("_links")
    val links: ApiLinks,
    @SerializedName("page")
    val page: ApiPage
) {

    data class ApiPage(
        @SerializedName("number")
        val number: Int,
        @SerializedName("size")
        val size: Int,
        @SerializedName("totalElements")
        val totalElements: Int,
        @SerializedName("totalPages")
        val totalPages: Int
    )

    data class ApiLinks(
        @SerializedName("profile")
        val profile: ApiProfile,
        @SerializedName("self")
        val self: ApiSelf
    ) {

        data class ApiProfile(
            @SerializedName("href")
            val href: String
        )
    }

    data class ApiEmbedded(
        @SerializedName("photos")
        val photos: List<ApiPhoto>
    ) {

        data class ApiPhoto(
            @SerializedName("_links")
            val links: ApiLinks,
            @SerializedName("uri")
            val uri: String,
            @SerializedName("label")
            val label: String?
        ) {

            val rawId
                get() = uri.substringAfterLast('/')

            data class ApiLinks(
                @SerializedName("photo")
                val photo: ApiPhoto,
                @SerializedName("self")
                val self: ApiSelf
            ) {

                data class ApiPhoto(
                    @SerializedName("href")
                    val href: String
                )
            }
        }
    }

    companion object {
        val EMPTY = ApiPhotos(
            embedded = ApiEmbedded(emptyList()),
            links = ApiLinks(ApiLinks.ApiProfile(""), ApiSelf("")),
            page = ApiPage(number = 0, size = 0, totalElements = 0, totalPages = 0)
        )
    }

    data class ApiSelf(
        @SerializedName("href")
        val href: String,
        @SerializedName("templated")
        val templated: Boolean? = null
    )
}
