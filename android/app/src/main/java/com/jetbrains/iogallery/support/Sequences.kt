package com.jetbrains.iogallery.support

inline fun <T, reified V : T> Sequence<T>.lastOfType(type: Class<V>): V {
    return last { type.isInstance(it) } as V
}
