package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Generation
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.calculateSavedMoney
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.domain.service.isLeapYear
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.readByteArray

val generationAverageSpendMoney: Map<Generation, Map<Gender, Money>> =
    mapOf(
        Generation.TEENAGERS to mapOf(Gender.MALE to Money(resolveSpendMoney(22000))),
        Generation.TEENAGERS to mapOf(Gender.FEMALE to Money(resolveSpendMoney(31000))),
        Generation.TWENTIES to mapOf(Gender.MALE to Money(resolveSpendMoney(64000))),
        Generation.TWENTIES to mapOf(Gender.FEMALE to Money(resolveSpendMoney(55000))),
        Generation.THIRTIES to mapOf(Gender.MALE to Money(resolveSpendMoney(76000))),
        Generation.THIRTIES to mapOf(Gender.FEMALE to Money(resolveSpendMoney(62000))),
        Generation.FORTIES to mapOf(Gender.MALE to Money(resolveSpendMoney(86000))),
        Generation.FORTIES to mapOf(Gender.FEMALE to Money(resolveSpendMoney(72000))),
        Generation.UNKNOWN to mapOf(Gender.MALE to Money(resolveSpendMoney(86000))),
        Generation.UNKNOWN to mapOf(Gender.FEMALE to Money(resolveSpendMoney(72000)))
    )

private fun resolveSpendMoney(value: Int): Int = value * 11 / 10

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
        calendarView(userService)
    }
    route("/api") {
        missionCategoriesRoutes()
    }
}

fun Route.missionCategoriesRoutes() {
    route("/mission-categories") {
        getMissionCategories()
    }
}

private fun Route.getMissionCategories() {
    get {
        val categories =
            MissionCategory.entries.map { category ->
                MissionCategoryResponse.from(category)
            }

        call.respond(
            HttpStatusCode.OK,
            MissionCategoriesResponse(categories)
        )
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
                user.preferredMissionCategoryList.map { MissionCategoryResponse.from(it) },
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

    val epochStart =
        Instant
            .fromEpochSeconds(0)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date // 1970-01-01 UTC
    val now = now()

    val daysFromEpoch = (now - epochStart).days

    val middle = COLOR.entries[daysFromEpoch % COLOR.entries.size]

    val last = ANIMAL.entries[daysFromEpoch % ANIMAL.entries.size]

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
    SPRING("봄"),
    SUMMER("여름"),
    AUTUMN("가을"),
    WINTER("겨울")
}

enum class COLOR(
    val description: String
) {
    GREEN("초록"),
    RED("붉은"),
    YELLOW("노란"),
    WHITE("흰"),
    BLACK("검정")
}

enum class ANIMAL(
    val description: String
) {
    MOUSE("쥐"),
    COW("소"),
    TIGER("호랑이"),
    RABBIT("토끼"),
    DRAGON("용"),
    SNAKE("뱀"),
    HORSE("말"),
    SHEEP("양"),
    MONKEY("원숭이"),
    ROOSTER("닭"),
    DOG("개"),
    PIG("돼지")
}

private fun Route.analysisView(userService: UserService) {
    get("/analysis") {
        val userId = call.pathParameters["userId"]!!

        val user = userService.getUserById(userId)

        val now = now()
        val year = now.year
        val month = now.month

        val baseYearMonth = LocalDate(year, month, 1)

        val weeklyPastRoutines = userService.getWeekPastRoutines(userId = userId, date = baseYearMonth)

        val weeklySavedMoney =
            weeklyPastRoutines
                .map { it.missions.calculateSavedMoney() }
                .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

        call.respond(
            HttpStatusCode.OK,
            AnalysisViewResponse(
                totalSavedMoney = user.totalSavedMoney,
                weeklySavedMoney = weeklySavedMoney,
                generationMoneyViewResponse =
                    GenerationMoneyViewResponse(
                        gender = user.gender,
                        generation = user.generation,
                        averageSpendMoney =
                            generationAverageSpendMoney[user.generation]!![user.gender] ?: Money(BigDecimal.ZERO)
                    )
            )
        )
    }
}

private fun Route.calendarView(userService: UserService) {
    get("/calendar") {
        val userId = call.pathParameters["userId"]!!

        val now = now()

        val yearMonthString =
            call.parameters["yearMonth"]
                ?: "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

        val (year, month) = yearMonthString.split("-").map { it.toInt() }
        val baseYearMonth = LocalDate(year, month, 1)

        val threeMonthViewResponse =
            (-1..1).map { addMonth ->
                // TODO: 시간 날 때 캐싱하기
                val currentMonthLocalDate: LocalDate = baseYearMonth.plus(addMonth, DateTimeUnit.MONTH)
                val monthlyPastRoutines =
                    userService.getMonthlyPastRoutines(userId = userId, date = currentMonthLocalDate)
                val monthlyFinishedPercentage =
                    monthlyPastRoutines
                        .filter { it.missions.isNotEmpty() }
                        .sumOf {
                            100 * it.missions.filter { mission -> mission.finished }.size /
                                (it.missions.size)
                        }

                val calendarDayMissionViews =
                    monthlyPastRoutines.map {
                        CalendarDayMissionView(
                            day = it.date.dayOfMonth,
                            date = it.date,
                            finishedMissionCount = it.missions.filter { mission -> mission.finished }.size,
                            totalMissionCount = it.missions.size
                        )
                    }

                val monthlyTotalPercentage =
                    if (addMonth == 0) {
                        (1..now.dayOfMonth).sum() * 100
                    } else {
                        val isLeapYear = currentMonthLocalDate.isLeapYear()
                        (1..currentMonthLocalDate.month.length(isLeapYear)).sum() * 100
                    }

                val averageSucceedProbability =
                    if (monthlyTotalPercentage == 0) 0 else monthlyFinishedPercentage / monthlyTotalPercentage
                AnalysisMonthViewResponse(
                    month = currentMonthLocalDate.month.value,
                    averageSucceedProbability = averageSucceedProbability,
                    calendarDayMissionViews = calendarDayMissionViews
                )
            }

        call.respond(HttpStatusCode.OK, CalendarViewResponse(threeMonthViewResponse))
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