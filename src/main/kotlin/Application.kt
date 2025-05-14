package com.mashup

import com.mashup.samples.configureAdministration
import com.mashup.samples.configureDatabases
import com.mashup.samples.configureHTTP
import com.mashup.samples.configureMonitoring
import com.mashup.samples.configureRouting
import com.mashup.samples.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureAdministration()
    configureDatabases()
    configureRouting()
}