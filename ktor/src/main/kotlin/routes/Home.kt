package com.jetbrains.ktorServer.routes

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get


fun Route.home() {
    get("/") {
        call.respondText("Welcome to Ktor!")
    }

}