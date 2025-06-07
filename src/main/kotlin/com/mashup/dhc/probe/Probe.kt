package com.mashup.dhc.probe

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.probeRoute() {
    get("/health") {
        call.respondText("UP")
    }
}