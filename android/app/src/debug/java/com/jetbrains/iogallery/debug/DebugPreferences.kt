package com.jetbrains.iogallery.debug

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.jetbrains.iogallery.api.ApiServer
import com.jetbrains.iogallery.api.ApiServer.GCP
import com.jetbrains.iogallery.api.ApiServer.SWAGGER
import timber.log.Timber
import java.util.*

class DebugPreferences(context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun subscribeToApiServer(owner: LifecycleOwner, onValueChanged: (ApiServer) -> Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key.equals(KEY_API_SERVER, ignoreCase = true)) {
                onValueChanged(apiServer)
            }
        }

        owner.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                owner.lifecycle.removeObserver(this)
            }
        })

        onValueChanged(apiServer)
    }

    private fun parseRawServerName(rawServerName: String) = when (rawServerName.toUpperCase(Locale.US)) {
        SWAGGER.name -> SWAGGER
        GCP.name -> GCP
        else -> {
            Timber.e("Invalid debug server setting found: $rawServerName, defaulting to SWAGGER")
            SWAGGER
        }
    }

    var apiServer: ApiServer
        get() = parseRawServerName(sharedPreferences.getString(KEY_API_SERVER, SWAGGER.name).orEmpty())
        @SuppressLint("ApplySharedPref") // We need to write immediately to disk so we can restart the process
        set(value) {
            sharedPreferences.edit()
                .putString(KEY_API_SERVER, value.name)
                .commit()
        }

    companion object {
        const val KEY_API_SERVER = "api_server"
    }
}
