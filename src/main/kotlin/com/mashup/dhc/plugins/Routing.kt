package com.mashup.com.mashup.dhc.plugins

import com.mashup.com.mashup.dhc.routes.ApiResponse
import com.mashup.com.mashup.dhc.routes.BusinessException
import com.mashup.com.mashup.dhc.routes.ErrorCode
import com.mashup.com.mashup.dhc.routes.InternalServerErrorException
import com.mashup.dhc.routes.sampleRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.util.reflect.typeInfo

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json() // kotlinx.serialization 사용 시
    }
    configureStatusPages()
    install(AutoHeadResponse)

    routing {
        sampleRoute()
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BusinessException> { call, cause ->
            call.response.status(cause.errorCode.httpStatus)
            call.respond(
                ApiResponse.error(cause.errorCode.code, cause.errorCode.message),
                typeInfo<ApiResponse<Unit>>()
            )
        }

        exception<InternalServerErrorException> { call, cause ->
            call.response.status(cause.errorCode.httpStatus)
            call.respond(
                ApiResponse.error(cause.errorCode.code, cause.errorCode.message),
                typeInfo<ApiResponse<Unit>>()
            )
        }

        exception<Throwable> { call, cause ->
            call.response.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus)
            call.respond(
                ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.code, cause.localizedMessage),
                typeInfo<ApiResponse<Unit>>()
            )
        }
    }
}
