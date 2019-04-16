package com.jetbrains.iogallery.model

import com.google.gson.annotations.SerializedName

data class ImageEntry(
    @field:SerializedName("id") val id: Long,
    @field:SerializedName("url") val url: String,
    @field:SerializedName("label") val label: String?
)
