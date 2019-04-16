package com.jetbrains.iogallery.support

import androidx.fragment.app.Fragment
import com.jetbrains.iogallery.model.PhotoId
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

fun Fragment.requiredPhotoIdsFromRawStringArray(key: String) = object : Lazy<List<PhotoId>> {

    private val cachedValue: List<PhotoId>? = null

    override val value: List<PhotoId>
        get() {
            if (isInitialized()) return cachedValue!!

            val arguments = requireArguments()
            require(arguments.containsKey(key)) { "The required argument '$key' is missing!" }
            return arguments.getStringArray(key)!!
                .map { rawId -> PhotoId(rawId) }
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

fun Fragment.requiredPhotoIdFromRawString(key: String): Lazy<PhotoId> = requiredTypedArgumentFromRawString(key) { PhotoId(it) }

private fun <T> Fragment.requiredTypedArgumentFromRawString(key: String, mapper: (rawValue: String) -> T) = object : Lazy<T> {

    private val cachedValue: T? = null

    override val value: T
        get() {
            if (isInitialized()) return cachedValue!!

            val arguments = requireArguments()
            require(arguments.containsKey(key)) { "The required argument '$key' is missing!" }
            return mapper(arguments.getString(key)!!)
        }

    override fun isInitialized() = cachedValue != null
}
