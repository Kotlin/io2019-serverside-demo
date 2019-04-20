@file: UseExperimental(KtorExperimentalLocationsAPI::class)
package com.jetbrains.ktorServer.routes

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route
import kotlinx.html.*

@Location("/share/{id}")
class share(val id: String)

fun Route.share() {
    get<share> { share ->
        call.respondHtml {
            body {
                h1 { +"Someone has shared a picture with you..." }
                br
                img(alt = share.id, src = "https://cloud-kotlin-io19.appspot.com/image/${share.id}")
            }
        }
    }
}