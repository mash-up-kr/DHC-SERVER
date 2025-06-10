package com.mashup.dhc.routes

import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import java.util.UUID

fun Route.sampleRoute() {
    route("/") {
        get {
            call.respondText("Hello DHC World!")
        }
    }
}

fun Route.storageRoutes(storage: NaverCloudPlatformObjectStorageAgent) {
    route("/api/storage") {
        // http://localhost:8080/api/storage/upload
        post("/upload") {
            val multipart = call.receiveMultipart()
            var displayLogo: String? = null
            var fileData: ByteArray? = null
            var contentType: String? = null
            var originalFileName: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "displayLogo") {
                            displayLogo = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        originalFileName = part.originalFileName ?: "file"
                        fileData = part.provider().readRemaining().readBytes()
                        contentType = part.contentType?.toString()
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (fileData == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("파일이 없습니다"))
                return@post
            }

            // displayLogo가 있으면 고정 키 사용, 없으면 랜덤 키 생성
            val key =
                if (!displayLogo.isNullOrBlank()) {
                    val extension = originalFileName?.substringAfterLast('.', "") ?: ""
                    "logos/${displayLogo}${if (extension.isNotEmpty()) ".$extension" else ""}"
                } else {
                    "uploads/${UUID.randomUUID()}_$originalFileName"
                }

            val url =
                storage.upload(
                    key = key,
                    data = fileData,
                    contentType = contentType
                )

            call.respond(HttpStatusCode.OK, UploadResponse(url))
        }

        // http://localhost:8080/api/storage/logos/eat-drink.png
        delete("/{key...}") {
            val key = call.parameters.getAll("key")?.joinToString("/")
            key?.let { storage.delete(it) }

            call.respond(HttpStatusCode.NoContent)
        }

        // http://localhost:8080/api/storage/url/logos/eat-drink.png
        get("/url/{key...}") {
            val key = call.parameters.getAll("key")?.joinToString("/")

            if (key.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("키가 필요합니다"))
                return@get
            }

            val url = storage.getUrl(key)
            call.respond(HttpStatusCode.OK, UploadResponse(url))
        }
    }
}