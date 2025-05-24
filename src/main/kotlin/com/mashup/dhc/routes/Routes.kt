package com.mashup.dhc.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.sampleRoute() {
    route("/") {
        get {
            call.respondText("Hello DHC World!")
        }
    }
}