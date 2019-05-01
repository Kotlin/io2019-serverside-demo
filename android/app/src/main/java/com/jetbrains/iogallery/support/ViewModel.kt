package com.jetbrains.iogallery.support

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

inline fun <reified T : ViewModel> Fragment.provideViewModel(): T =
    ViewModelProviders.of(this).get(T::class.java)

inline fun <reified T : ViewModel> Fragment.provideViewModel(crossinline factory: () -> T): T =
    ViewModelProviders.of(
        this,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") // Magic at work here
            override fun <V : ViewModel> create(modelClass: Class<V>): V = factory() as V
        }
    ).get(T::class.java)
