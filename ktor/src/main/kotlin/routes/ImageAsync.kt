package com.jetbrains.ktorServer.routes

import com.jetbrains.ktorServer.StorageResult
import com.jetbrains.ktorServer.loadFromStorage
import com.jetbrains.ktorServer.saveToStorage
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.put
import io.ktor.response.respond
import io.ktor.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.convertToMonoChromeAsync() {
    put<mono> {
        val imageId = "demo/${it.id}"
        val loadResult = call.loadFromStorage("cloud-kotlin-io19", imageId)
        when (loadResult) {
            is StorageResult.LoadSuccess -> {
                withContext(Dispatchers.IO) {
                    val monochrome = monochrome(loadResult.data)
                    val saveResult = call.saveToStorage("cloud-kotlin-io19", imageId, monochrome)
                    when (saveResult) {
                        is StorageResult.SaveSucess -> call.respond(HttpStatusCode.OK)
                        is StorageResult.Failure -> call.respond(HttpStatusCode.InternalServerError, saveResult.message)
                    }
                }
            }
            is StorageResult.Failure -> {
                call.respond(HttpStatusCode.InternalServerError, loadResult.message)
            }
        }
    }
}