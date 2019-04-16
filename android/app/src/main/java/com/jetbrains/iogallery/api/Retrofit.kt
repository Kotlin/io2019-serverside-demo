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
import java.util.concurrent.atomic.AtomicBoolean

private val okHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .apply {
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BASIC
            addInterceptor(interceptor)
        }
    }
    .build()

fun retrofit(apiServer: ApiServer): Retrofit = Retrofit.Builder()
    .baseUrl(apiServer.baseUrl)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(LiveDataCallAdapterFactory)
    .build()

enum class ApiServer(val baseUrl: String) {
    SWAGGER("https://virtserver.swaggerhub.com/seebrock3r/io-gallery/1.0.0/"),
    GCP("https://cloud-kotlin-io19.appspot.com/")
}

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
                            postValue(Result.failure(HttpException(response)))
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
