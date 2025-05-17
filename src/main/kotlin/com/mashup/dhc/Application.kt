package com.mashup.dhc

import com.mashup.com.mashup.dhc.plugins.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureRouting()
}