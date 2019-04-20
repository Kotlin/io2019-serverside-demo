package com.jetbrains.ktorServer.routes

import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route



// TODO: Maybe replace this with CRUD example

fun Route.listData() {
    route("/data") {
        get("/all") {

        }

        get("/unprocessed") {

        }

    }
}