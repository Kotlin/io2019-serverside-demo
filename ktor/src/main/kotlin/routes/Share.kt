@file: UseExperimental(KtorExperimentalLocationsAPI::class)
package routes

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route
import kotlinx.html.*

@Location("/share/{id}") class share(val id: String)

fun Route.share() {
    get<share> {
        call.respondHtml {
            body {
                h1 { +"Someone has shared a picture with you..."}
                br
                div {
                    +"Picture goes here"
                }
            }
        }
    }
}