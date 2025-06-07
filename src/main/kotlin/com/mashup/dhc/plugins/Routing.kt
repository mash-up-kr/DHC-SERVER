package com.mashup.com.mashup.dhc.plugins

import com.mashup.dhc.probe.probeRoute
import com.mashup.dhc.routes.sampleRoute
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    install(AutoHeadResponse)
    routing {
        sampleRoute()
        probeRoute()
    }
}