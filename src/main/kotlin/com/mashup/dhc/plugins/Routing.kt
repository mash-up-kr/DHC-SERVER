package com.mashup.dhc.plugins

import com.mashup.dhc.probe.probeRoute
import com.mashup.dhc.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        probeRoute()
        userRoutes(dependencies.userService, dependencies.fortuneService)
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        // 정적 파일 제공
        staticResources("/static", "static")
    }
}