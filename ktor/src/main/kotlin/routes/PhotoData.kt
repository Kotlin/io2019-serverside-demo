package com.jetbrains.ktorServer.routes

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery
import com.jetbrains.ktorServer.queryDataStore
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route


data class Photo(val id: String, val label: String, val uri: String)

fun Route.listData() {
    route("/data") {
        get("/all") {
            val photos = queryDataStore()
            call.respond(photos)
        }
        get("/unprocessed") {
            val photos = queryDataStore(true)
            call.respond(photos)
        }
    }
}

