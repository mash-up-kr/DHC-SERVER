package com.mashup.dhc.plugins

import com.mashup.dhc.probe.probeRoute
import com.mashup.dhc.routes.storageRoutes
import com.mashup.dhc.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        probeRoute()
        userRoutes(dependencies.userService, dependencies.fortuneService)
        storageRoutes(dependencies.storage)
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}