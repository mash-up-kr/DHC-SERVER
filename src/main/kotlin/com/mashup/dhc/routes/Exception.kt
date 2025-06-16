package com.mashup.dhc.routes

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)

class InternalServerErrorException(
    val errorCode: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR
) : RuntimeException(errorCode.message)

@Serializable
enum class ErrorCode(
    val code: Int,
    val message: String,
    val httpStatus: HttpStatusCode
) {
    // Client Errors
    INVALID_REQUEST(1001, "Invalid request", HttpStatusCode.BadRequest),
    UNAUTHORIZED(2001, "Unauthorized access", HttpStatusCode.Unauthorized),
    FORBIDDEN(2002, "Forbidden", HttpStatusCode.Forbidden),
    NOT_FOUND(3001, "Resource not found", HttpStatusCode.NotFound),

    // Server Errors
    INTERNAL_SERVER_ERROR(5000, "Internal server error", HttpStatusCode.InternalServerError),
    DATABASE_ERROR(5001, "Database error", HttpStatusCode.InternalServerError)
}