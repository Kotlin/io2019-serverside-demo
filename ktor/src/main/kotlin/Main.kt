@file:UseExperimental(KtorExperimentalLocationsAPI::class)

package com.jetbrains.ktorServer


import com.jetbrains.ktorServer.routes.home
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.locations
import io.ktor.routing.routing
import routes.convertToMonoChrome
import routes.listData
import routes.share


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
