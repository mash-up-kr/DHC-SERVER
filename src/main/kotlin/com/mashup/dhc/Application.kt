package com.mashup.dhc

import com.mashup.dhc.plugins.configureDependencies
import com.mashup.dhc.plugins.configurePlugins
import com.mashup.dhc.plugins.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    // 플러그인 설정
    configurePlugins()

    // 의존성 주입, 라우팅 설정
    val dependencies = configureDependencies()
    configureRouting(dependencies)
}