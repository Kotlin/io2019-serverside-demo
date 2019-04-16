package com.jetbrains.iogallery.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") // Magic at work here
        override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
    }
