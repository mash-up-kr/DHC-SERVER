package com.mashup.dhc.plugins

import com.mashup.dhc.routes.BusinessException
import com.mashup.dhc.routes.ErrorCode
import com.mashup.dhc.routes.ErrorResponse
import com.mashup.dhc.routes.InternalServerErrorException
import com.mashup.dhc.routes.ValidationErrorResponse
import com.mashup.dhc.routes.ValidationException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(AutoHeadResponse)

    // CallLogging 플러그인
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            "$httpMethod $path - $status - $userAgent"
        }
    }

    // StatusPages 플러그인 - 에러 처리
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.application.log.warn("Validation failed: ${cause.errorMessages}")
            call.respond(
                cause.errorCode.httpStatus,
                ValidationErrorResponse.from(cause.errors)
            )
        }

        exception<BusinessException> { call, cause ->
            call.application.log.warn("Business error: ${cause.errorCode}")
            call.respond(
                cause.errorCode.httpStatus,
                ErrorResponse.from(cause.errorCode)
            )
        }

        exception<InternalServerErrorException> { call, cause ->
            call.application.log.error("Internal server error", cause)
            call.respond(
                cause.errorCode.httpStatus,
                ErrorResponse.from(cause.errorCode)
            )
        }

        exception<BadRequestException> { call, cause ->
            val errorCode =
                when (val rootCause = cause.cause) {
                    is SerializationException -> {
                        val message = rootCause.message ?: "Unknown serialization error"
                        when {
                            message.contains("Fields") && message.contains("are required") -> {
                                ErrorCode.MISSING_REQUIRED_FIELD
                            }

                            message.contains("Unexpected JSON token") -> {
                                ErrorCode.INVALID_JSON_FORMAT
                            }

                            message.contains("Enum value") || message.contains("Enum class") -> {
                                ErrorCode.INVALID_FIELD_VALUE
                            }

                            else -> ErrorCode.INVALID_REQUEST
                        }
                    }

                    else -> ErrorCode.INVALID_REQUEST
                }

            val details = cause.cause?.message ?: cause.message
            call.application.log.warn("Bad request: $errorCode - $details")
            call.respond(
                errorCode.httpStatus,
                ErrorResponse.from(errorCode, details)
            )
        }

        exception<SerializationException> { call, cause ->
            call.application.log.warn("Serialization error: ${cause.message}")
            call.respond(
                ErrorCode.INVALID_JSON_FORMAT.httpStatus,
                ErrorResponse.from(ErrorCode.INVALID_JSON_FORMAT, cause.message)
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception occurred", cause)
            call.respond(
                ErrorCode.INTERNAL_SERVER_ERROR.httpStatus,
                ErrorResponse.from(
                    ErrorCode.INTERNAL_SERVER_ERROR
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse.from(ErrorCode.NOT_FOUND)
            )
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                ErrorResponse.from(ErrorCode.UNAUTHORIZED)
            )
        }

        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(
                status,
                ErrorResponse.from(ErrorCode.FORBIDDEN)
            )
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
}