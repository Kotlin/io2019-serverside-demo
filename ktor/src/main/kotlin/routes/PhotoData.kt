package routes

import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route


fun Route.listData() {
    route("/data") {
        get("/all") {

        }

        get("/unprocessed") {

        }

    }
}