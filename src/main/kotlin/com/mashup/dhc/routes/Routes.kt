package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.calculateSavedMoney
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.domain.service.now
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mashup.dhc.utils.Money
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import java.math.BigDecimal
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.readByteArray

fun Route.userRoutes(userService: UserService) {
    route("/api/users") {
        register(userService)
        changeMissionStatus(userService)
        endToday(userService)
        logout(userService)
    }
    route("/view/users/{userId}") {
        home(userService)
        myPage(userService)
        analysisView(userService)
    }
}

private fun Route.register(userService: UserService) {
    post("/register") {
        val request = call.receive<RegisterUserRequest>()
        application.log.info(
            "Received register request: userToken=${request.userToken}, gender=${request.gender}, birthDate=${request.birthDate}, birthTime=${request.birthTime}, categories=${request.preferredMissionCategoryList}"
        )

        request.validate()

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
            throw BusinessException(ErrorCode.USER_ALREADY_EXISTS)
        }
    }
}

private fun Route.home(userService: UserService) {
    get("/home") {
        val userId = call.pathParameters["userId"] ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        val user = userService.getUserById(userId)

        val now =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        call.respond(
            HttpStatusCode.OK,
            HomeViewResponse(
                longTermMission = user.longTermMission?.let { MissionResponse.from(it) },
                todayDailyMissionList = user.todayDailyMissionList.map { MissionResponse.from(it) },
                todayDailyFortune =
                    user.monthlyFortuneList
                        .lastOrNull()
                        ?.dailyFortuneList
                        ?.find { it.date == now.toString() }
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
        endDate = this.endDate!!,
        title = this.title,
        switchCount = this.switchCount
    )

private fun Route.endToday(userService: UserService) {
    post("/{userId}/done") {
        val userId =
            call.pathParameters["userId"]
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        val request = call.receive<EndTodayMissionRequest>()

        request.validate()

        val todaySavedMoney =
            userService.summaryTodayMission(
                userId,
                request.date
            )

        call.respond(HttpStatusCode.OK, EndTodayMissionResponse(todaySavedMoney))
    }
}

private fun Route.changeMissionStatus(userService: UserService) {
    put("/{userId}/missions/{missionId}") {
        val userId =
            call.pathParameters["userId"]
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)
        val missionId =
            call.pathParameters["missionId"]
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        val request = call.receive<ToggleMissionRequest>()

        try {
            request.validate()
        } catch (e: ValidationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationErrorResponse.from(e.errors)
            )
            return@put
        }

        val updated =
            if (request.finished != null) {
                userService.updateTodayMission(userId, missionId, request.finished)
            } else {
                userService.switchTodayMission(userId, missionId)
            }

        val mission =
            (updated.todayDailyMissionList + updated.longTermMission)
                .filterNotNull()
                .find { it.id.toString() == missionId }
                ?: throw BusinessException(ErrorCode.MISSION_NOT_FOUND)

        call.respond(HttpStatusCode.OK, ToggleMissionResponse(MissionResponse.from(mission)))
    }
}

private fun Route.myPage(userService: UserService) {
    get("/myPage") {
        val userId = call.pathParameters["userId"]!!
        val user = userService.getUserById(userId)

        call.respond(
            HttpStatusCode.OK,
            MyPageResponse(
                user.resolveAnimalCard(),
                user.birthDate,
                user.birthTime,
                user.preferredMissionCategoryList,
                true // TODO: alarm
            )
        )
    }
}

private fun User.resolveAnimalCard(): AnimalCard {
    val first =
        when (this.birthDate.date.month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> SEASON.SPRING
            Month.MARCH, Month.APRIL, Month.MAY -> SEASON.SUMMER
            Month.JUNE, Month.JULY, Month.AUGUST -> SEASON.AUTUMN
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> SEASON.WINTER
        }

    val middle = COLOR.WHITE

    val last = ANIMAL.HORSE

    return AnimalCard(
        name = "${first.description}의 ${middle.description} ${last.description}",
        cardImageUrl = ""
    )
}

private fun Route.logout(userService: UserService) {
    delete("/{userId}") {
        val userId = call.pathParameters["userId"]!!
        userService.deleteById(userId)

        call.respond(HttpStatusCode.OK)
    }
}

enum class SEASON(
    val description: String
) {
    SPRING("봄의"),
    SUMMER("여름의"),
    AUTUMN("가을의"),
    WINTER("겨울의")
}

enum class COLOR(
    val description: String
) {
    BLUE("푸른"),
    RED("붉은"),
    YELLOW("노란"),
    WHITE("흰"),
    BLACK("검정")
}

enum class ANIMAL(
    val description: String
) {
    HORSE("말")
}

private fun Route.analysisView(userService: UserService) {
    get("/analysis") {
        val userId = call.pathParameters["userId"]!!

        val user = userService.getUserById(userId)
        val now = now()

        val monthlyPastRoutines = userService.getMonthlyPastRoutines(userId = userId, date = now)
        val weeklyPastRoutines = userService.getWeekPastRoutines(userId = userId, date = now)

        val weeklySavedMoney =
            weeklyPastRoutines
                .map { it.missions.calculateSavedMoney() }
                .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

        val monthlyFinishedPercentage =
            monthlyPastRoutines
                .filter { it.missions.isNotEmpty() }
                .sumOf {
                    100 * it.missions.filter { mission -> mission.finished }.size /
                        (it.missions.size)
                }

        val monthlyTotalPercentage = (1..now.dayOfMonth).sum() * 100

        call.respond(
            HttpStatusCode.OK,
            AnalysisViewResponse(
                totalSavedMoney = user.totalSavedMoney,
                weeklySavedMoney = weeklySavedMoney,
                averageSucceedProbability = monthlyFinishedPercentage / monthlyTotalPercentage,
                calendarDayMissionViews =
                    monthlyPastRoutines.map {
                        CalendarDayMissionView(
                            day = it.date.dayOfMonth,
                            date = it.date,
                            finishedMissionCount = it.missions.filter { mission -> mission.finished }.size,
                            totalMissionCount = it.missions.size
                        )
                    }
            )
        )
    }
}

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
            throw BusinessException(ErrorCode.FILE_NOT_FOUND)
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
        val key =
            call.parameters.getAll("key")?.joinToString("/")
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        storage.delete(key)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.getFileUrl(storage: NaverCloudPlatformObjectStorageAgent) {
    get("/url/{key...}") {
        val key = call.parameters.getAll("key")?.joinToString("/")

        if (key.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        val url = storage.getUrl(key)
        call.respond(HttpStatusCode.OK, UploadResponse(url))
    }
}