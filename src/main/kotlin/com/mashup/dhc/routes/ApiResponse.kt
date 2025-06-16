package com.mashup.dhc.routes

import io.ktor.server.routing.RoutingCall
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> =
            ApiResponse(
                code = 200,
                message = "Success",
                data = data
            )

        fun <T> error(
            code: Int,
            message: String,
            data: T
        ): ApiResponse<T> =
            ApiResponse(
                code = code,
                message = message,
                data = data
            )

        fun error(
            code: Int,
            message: String
        ): ApiResponse<Unit> = ApiResponse(code, message, null)
    }
}

fun <T> RoutingCall.respondSuccess(data: T): ApiResponse<T> = ApiResponse.success(data)