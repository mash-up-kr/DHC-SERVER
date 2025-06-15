package com.mashup.dhc.plugins

import com.mashup.dhc.probe.probeRoute
import com.mashup.dhc.routes.storageRoutes
import com.mashup.dhc.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        // 헬스체크
        probeRoute()
        userRoutes(dependencies.userService)
        storageRoutes(dependencies.storage)
    }
}