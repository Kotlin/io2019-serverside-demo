package com.jetbrains.iogallery.api

import androidx.lifecycle.LiveData
import com.jetbrains.iogallery.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val okHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .connectTimeout(30L, TimeUnit.SECONDS)
    .readTimeout(30L, TimeUnit.SECONDS)
    .writeTimeout(30L, TimeUnit.SECONDS)
    .apply {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BASIC
            addInterceptor(interceptor)
        }

        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Authorization", BuildConfig.AUTH_HEADER)
                .build()
            chain.proceed(request)
        }
    }
    .build()

const val BASE_URI = "https://cloud-kotlin-io19.appspot.com/"
const val SHARE_BASE_URI = "https://20190417t135928-dot-cloud-kotlin-io19.appspot.com/"

fun retrofit(): Retrofit = Retrofit.Builder()
    .baseUrl(BASE_URI)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(LiveDataCallAdapterFactory)
    .build()

private class LiveDataCallAdapter<R>(private val responseType: Type) : CallAdapter<R, LiveData<Result<R>>> {

    override fun responseType() = responseType

    override fun adapt(call: Call<R>) = object : LiveData<Result<R>>() {
        var started = AtomicBoolean(false)

        override fun onActive() {
            super.onActive()

            if (started.compareAndSet(false, true)) {
                call.enqueue(object : Callback<R> {
                    override fun onResponse(call: Call<R>, response: Response<R>) {
                        if (response.isSuccessful) {
                            postValue(Result.success(response.body()!!))
                        } else {
                            val request = call.request()
                            val exception = RuntimeException("Error in request ${request.method()} ${request.url()}", HttpException(response))
                            postValue(Result.failure(exception))
                        }
                    }

                    override fun onFailure(call: Call<R>, throwable: Throwable) {
                        postValue(Result.failure(throwable))
                    }
                })
            }
        }
    }
}

private object LiveDataCallAdapterFactory : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != LiveData::class.java) {
            return null
        }

        val observableType = getParameterUpperBound(0, returnType as ParameterizedType)
        val rawObservableType = getRawType(observableType)
        if (rawObservableType != Result::class.java || observableType !is ParameterizedType) {
            throw IllegalArgumentException("Return type must be a Result<T>")
        }

        val bodyType = getParameterUpperBound(0, observableType)
        return LiveDataCallAdapter<Any>(bodyType)
    }
}
