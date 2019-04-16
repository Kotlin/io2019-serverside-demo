package com.jetbrains.iogallery.support

import android.content.Context
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso

fun Context.setupPicasso() {
    if (setupDone) return

    Picasso.setSingletonInstance(
        Picasso.Builder(this)
            .memoryCache(LruCache(this))
            .build()
    )

    setupDone = true
}

private var setupDone = false
