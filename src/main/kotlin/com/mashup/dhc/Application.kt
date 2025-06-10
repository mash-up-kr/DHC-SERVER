package com.mashup.dhc

import com.mashup.com.mashup.dhc.plugins.configureRouting
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mashup.dhc.routes.sampleRoute
import com.mashup.dhc.routes.storageRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    // JSON Serial/Deserial 자동화
    install(ContentNegotiation) {
        json()
    }

    val storage =
        NaverCloudPlatformObjectStorageAgent(
            accessKey = environment.config.property("ncp.accessKey").getString(),
            secretKey = environment.config.property("ncp.secretKey").getString(),
            bucketName = environment.config.property("ncp.bucketName").getString()
        )

    configureRouting()

    routing {
        sampleRoute()
        storageRoutes(storage)
    }
}