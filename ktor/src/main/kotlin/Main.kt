@file:UseExperimental(KtorExperimentalLocationsAPI::class)

package com.jetbrains.ktorServer


import com.jetbrains.ktorServer.routes.*
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.routing.route
import io.ktor.routing.routing


fun Application.main() {

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
    }
    install(Locations)
    routing {
        home()
        convertToMonoChrome()
        listData()
        share()
    }
}

