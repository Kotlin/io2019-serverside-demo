package com.jetbrains.iogallery.support

import android.content.Context
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso

fun Context.setupPicasso() {
    if (setupDone) return

    memoryCache = LruCache(this)
    Picasso.setSingletonInstance(
        Picasso.Builder(this)
            .memoryCache(memoryCache!!)
            .build()
    )

    setupDone = true
}

fun nukePicassoCache() {
    memoryCache?.clear()
}

private var memoryCache: LruCache? = null

private var setupDone = false
