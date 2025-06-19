package com.mashup.dhc.plugins

import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json()
    }
    install(AutoHeadResponse)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
}