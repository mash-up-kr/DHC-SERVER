package com.mashup.com.mashup.dhc.plugins

import com.mashup.com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.routes.register
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun Application.configureRouting(userService: UserService) {
    install(AutoHeadResponse)
    install(ContentNegotiation) { json() }
    routing {
        register(userService)
    }
}