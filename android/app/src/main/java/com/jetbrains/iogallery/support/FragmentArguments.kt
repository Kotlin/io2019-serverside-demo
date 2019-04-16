package com.jetbrains.iogallery.support

import androidx.fragment.app.Fragment
import java.io.Serializable

fun Fragment.requiredLongArgument(key: String) = object : Lazy<Long> {

    private val cachedValue: Long? = null

    override val value: Long
        get() {
            if (isInitialized()) return cachedValue!!

            val arguments = requireArguments()
            require(arguments.containsKey(key)) { "The required argument '$key' is missing!" }
            return arguments.getLong(key)
        }

    override fun isInitialized() = cachedValue != null
}

fun Fragment.requiredLongArrayArgument(key: String) = object : Lazy<LongArray> {

    private val cachedValue: LongArray? = null

    override val value: LongArray
        get() {
            if (isInitialized()) return cachedValue!!

            val arguments = requireArguments()
            require(arguments.containsKey(key)) { "The required argument '$key' is missing!" }
            return arguments.getLongArray(key)!!
        }

    override fun isInitialized() = cachedValue != null
}

fun <T : Serializable> Fragment.requiredSerializableArgument(key: String) = object : Lazy<T> {

    private val cachedValue: T? = null

    override val value: T
        get() {
            if (isInitialized()) return cachedValue!!

            val arguments = requireArguments()
            require(arguments.containsKey(key)) { "The required argument '$key' is missing!" }

            @Suppress("UNCHECKED_CAST") // We can't avoid this to provide a typed API
            return arguments.getSerializable(key) as T
        }

    override fun isInitialized() = cachedValue != null
}
