package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Money
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable

fun Route.userRoutes(userService: UserService) {
    route("/api/users") {
        register(userService)
        changeMissionStatus(userService)
        endToday(userService)
    }
    route("/view/users/{userId}") {
        home(userService)
    }
}

private fun Route.register(userService: UserService) {
    post("/register") {
        val request = call.receive<RegisterUserRequest>()

        val registeredUserId =
            userService.registerUser(
                request.userToken,
                request.gender,
                request.birthDate,
                request.birthTime,
                request.preferredMissionCategoryList
            )

        if (registeredUserId != null) {
            call.respond(HttpStatusCode.Created, RegisterUserResponse(registeredUserId.toString()))
        } else {
            call.respondText(
                "Failed to register user",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}

@Serializable
data class RegisterUserRequest(
    val userToken: String,
    val gender: Gender,
    val birthDate: BirthDate,
    val birthTime: BirthTime?,
    val preferredMissionCategoryList: List<MissionCategory>
)

@Serializable
data class RegisterUserResponse(
    val id: String
)

private fun Route.home(userService: UserService) {
    get("/home") {
        val userId = call.pathParameters["userId"]!!
        val user = userService.getUserById(userId)
        val now =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        call.respond(
            HttpStatusCode.OK,
            HomeViewResponse(
                longTermMission = user.longTermMission?.toMissionResponse(),
                todayDailyMissionList = user.todayDailyMissionList.map { it.toMissionResponse() },
                todayDailyFortune =
                    user.monthlyFortuneList
                        .lastOrNull()
                        ?.dailyFortuneList
                        ?.find { it.date == now.toString() } // toString == "yyyy-MM-dd"
            )
        )
    }
}

private fun Mission.toMissionResponse() =
    MissionResponse(
        missionId = this.id.toString(),
        category = this.category,
        difficulty = this.difficulty,
        type = this.type,
        finished = this.finished,
        cost = this.cost,
        endDate = this.endDate!!
    )

@Serializable
data class MissionResponse(
    val missionId: String,
    val category: MissionCategory,
    val difficulty: Int,
    val type: MissionType,
    val finished: Boolean = false,
    val cost: Money,
    val endDate: LocalDate
)

@Serializable
data class HomeViewResponse(
    val longTermMission: MissionResponse?,
    val todayDailyMissionList: List<MissionResponse>,
    val todayDailyFortune: DailyFortune?
)

private fun Route.endToday(userService: UserService) {
    post("/{userId}/done") {
        val userId = call.pathParameters["userId"]!!
        val request = call.receive<EndTodayMissionRequest>()

        val todaySavedMoney =
            userService.summaryTodayMission(
                userId,
                request.date
            )

        call.respond(HttpStatusCode.OK, EndTodayMissionResponse(todaySavedMoney))
    }
}

@Serializable
data class EndTodayMissionRequest(
    val date: LocalDate
)

@Serializable
data class EndTodayMissionResponse(
    val todaySavedMoney: Money
)

private fun Route.changeMissionStatus(userService: UserService) {
    put("/{userId}/missions/{missionId}") {
        val userId = call.pathParameters["userId"]!!
        val missionId = call.pathParameters["missionId"]!!

        val request = call.receive<ToggleMissionRequest>()

        val updated = userService.updateTodayMission(userId, missionId, request.finished)

        val response =
            (updated.todayDailyMissionList + updated.longTermMission)
                .filterNotNull()
                .find { it.id.toString() == missionId }!!
                .let { ToggleMissionResponse(it.toMissionResponse()) }

        call.respond(HttpStatusCode.OK, response)
    }
}

data class ToggleMissionRequest(
    val finished: Boolean
)

@Serializable
data class ToggleMissionResponse(
    val mission: MissionResponse
)

fun Route.storageRoutes(storage: NaverCloudPlatformObjectStorageAgent) {
    route("/api/storage") {
        uploadFile(storage)
        deleteFile(storage)
        getFileUrl(storage)
    }
}

private fun Route.uploadFile(storage: NaverCloudPlatformObjectStorageAgent) {
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
                    fileData = part.provider().readRemaining().readByteArray()
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
}

private fun Route.deleteFile(storage: NaverCloudPlatformObjectStorageAgent) {
    delete("/{key...}") {
        val key = call.parameters.getAll("key")?.joinToString("/")
        if (key != null) {
            storage.delete(key)
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("키가 필요합니다"))
        }
    }
}

private fun Route.getFileUrl(storage: NaverCloudPlatformObjectStorageAgent) {
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

@Serializable
data class UploadResponse(
    val url: String
)

@Serializable
data class ErrorResponse(
    val error: String
)