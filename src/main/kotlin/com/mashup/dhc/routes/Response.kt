package com.mashup.dhc.routes

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val url: String
)

@Serializable
data class ErrorResponse(
    val error: String
)