package com.jetbrains.iogallery.support

import android.content.Context
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso

fun Context.picasso(): Picasso =
    Picasso.Builder(this)
        .memoryCache(LruCache(this))
        .build()
