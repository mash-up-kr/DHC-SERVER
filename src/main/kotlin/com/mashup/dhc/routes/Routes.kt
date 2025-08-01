package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Generation
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.calculateSavedMoney
import com.mashup.dhc.domain.model.calculateSpendMoney
import com.mashup.dhc.domain.service.FortuneService
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.domain.service.isLeapYear
import com.mashup.dhc.domain.service.now
import com.mashup.dhc.domain.service.toGeminiFortuneRequest
import com.mashup.dhc.utils.Image
import com.mashup.dhc.utils.ImageFormat
import com.mashup.dhc.utils.ImageUrlMapper
import com.mashup.dhc.utils.Money
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.math.BigDecimal
import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

val generationAverageSpendMoney: Map<Generation, Map<Gender, Money>> =
    mapOf(
        Generation.TEENAGERS to
            mapOf(
                Gender.MALE to Money(resolveSpendMoney(22000)),
                Gender.FEMALE to Money(resolveSpendMoney(31000))
            ),
        Generation.TWENTIES to
            mapOf(
                Gender.MALE to Money(resolveSpendMoney(64000)),
                Gender.FEMALE to Money(resolveSpendMoney(55000))
            ),
        Generation.THIRTIES to
            mapOf(
                Gender.MALE to Money(resolveSpendMoney(76000)),
                Gender.FEMALE to Money(resolveSpendMoney(62000))
            ),
        Generation.FORTIES to
            mapOf(
                Gender.MALE to Money(resolveSpendMoney(86000)),
                Gender.FEMALE to Money(resolveSpendMoney(72000))
            ),
        Generation.UNKNOWN to
            mapOf(
                Gender.MALE to Money(resolveSpendMoney(86000)),
                Gender.FEMALE to Money(resolveSpendMoney(72000))
            )
    )

private fun resolveSpendMoney(value: Int): Int =
    if (now().dayOfMonth % 2 == 1) {
        value.plusTenPercent()
    } else {
        value.minusTenPercent()
    }

private fun Int.plusTenPercent() = this * 11 / 10

private fun Int.minusTenPercent() = this * 9 / 10

fun Route.userRoutes(
    userService: UserService,
    fortuneService: FortuneService
) {
    route("/api/users") {
        register(userService)
        changeMissionStatus(userService)
        endToday(userService)
        logout(userService)
        searchUser(userService)
        getDailyFortune(fortuneService)
        addJulyPastRoutineHistory(userService)
    }
    route("/view/users/{userId}") {
        home(userService, fortuneService)
        myPage(userService)
        analysisView(userService)
        calendarView(userService)
    }
    route("/api") {
        missionCategoriesRoutes()
    }
}

fun Route.searchUser(userService: UserService) {
    get {
        val userToken: String? = call.request.queryParameters["userToken"]

        if (userToken.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }

        val user = userService.findUserByUserToken(userToken)

        if (user?.id == null) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND)
        }

        call.respond(HttpStatusCode.OK, SearchUserResponse(user.id.toHexString()))
    }
}

fun Route.missionCategoriesRoutes() {
    route("/mission-categories") {
        getMissionCategories()
    }
}

private fun Route.getMissionCategories() {
    get {
        val formatParam = call.request.queryParameters["format"] ?: "svg"
        val format =
            try {
                ImageFormat.valueOf(formatParam.uppercase())
            } catch (e: IllegalArgumentException) {
                ImageFormat.SVG
            }

        val categories =
            MissionCategory.entries
                .filter { category -> category != MissionCategory.SELF_REFLECTION }
                .map { category -> MissionCategoryResponse.from(category, format) }

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
            call.respond(HttpStatusCode.Created, RegisterUserResponse(registeredUserId.toHexString()))
        } else {
            throw BusinessException(ErrorCode.USER_ALREADY_EXISTS)
        }
    }
}

private fun Route.home(
    userService: UserService,
    fortuneService: FortuneService
) {
    get("/home") {
        val userId = call.pathParameters["userId"] ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        var user = userService.getUserById(userId)

        val now =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        val todayPastRoutines = userService.getTodayPastRoutines(userId, now)

        val todayDailyFortune = fortuneService.queryDailyFortune(userId, now)

        val isAlreadyAllDone = user.todayDailyMissionList.any { it.endDate?.run { this <= now } ?: false }
        if (isAlreadyAllDone) {
            userService.summaryTodayMission(
                userId,
                user.todayDailyMissionList.random().endDate!!
            )
            user = userService.getUserById(userId) // 유저 정보 갱신
        }

        if (user.dailyFortunes == null || user.dailyFortunes.all { LocalDate.parse(it.date) < now }) {
            fortuneService.enqueueGenerateDailyFortuneTask(
                user.id.toString(),
                user.toGeminiFortuneRequest()
            )
        }

        call.respond(
            HttpStatusCode.OK,
            HomeViewResponse(
                longTermMission = user.longTermMission?.let { MissionResponse.from(it) },
                todayDailyMissionList = user.todayDailyMissionList.map { MissionResponse.from(it) },
                todayDailyFortune = todayDailyFortune.let { FortuneResponse.from(it) }, // FortuneResponse로 변환
                todayDone = todayPastRoutines.isNotEmpty()
            )
        )
    }
}

private fun Mission.toMissionResponse() =
    MissionResponse(
        missionId = this.id.toString(),
        category = this.category.displayName,
        difficulty = this.difficulty,
        type = this.type,
        finished = this.finished,
        cost = this.cost,
        endDate = this.endDate,
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

        val longTermMission =
            if (updated.longTermMission != null) {
                listOf(updated.longTermMission)
            } else {
                listOf()
            }
        val missions =
            updated.todayDailyMissionList + longTermMission

        call.respond(HttpStatusCode.OK, ToggleMissionResponse(missions.map { it.toMissionResponse() }))
    }
}

private fun Route.myPage(userService: UserService) {
    get("/myPage") {
        val userId = call.pathParameters["userId"]!!
        val user = userService.getUserById(userId)

        val formatParam = call.request.queryParameters["format"] ?: "svg"
        val format =
            try {
                ImageFormat.valueOf(formatParam.uppercase())
            } catch (e: IllegalArgumentException) {
                ImageFormat.SVG
            }

        call.respond(
            HttpStatusCode.OK,
            MyPageResponse(
                user.resolveAnimalCard(format),
                user.birthDate,
                user.birthTime,
                user.preferredMissionCategoryList.map { MissionCategoryResponse.from(it, format) },
                true // TODO: alarm
            )
        )
    }
}

private fun User.resolveAnimalCard(format: ImageFormat = ImageFormat.SVG): AnimalCard {
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
            .date
    val nowDays = this.birthDate.date

    val daysFromEpoch = abs((nowDays - epochStart).days)

    val middle = COLOR.entries[daysFromEpoch % COLOR.entries.size]
    val last = ANIMAL.entries[daysFromEpoch % ANIMAL.entries.size]

    return AnimalCard(
        name = "${first.description}의 ${middle.description} ${last.description}",
        cardImage = generateAnimalCardImageUrl(middle, last, format)
    )
}

private fun generateAnimalCardImageUrl(
    color: COLOR,
    animal: ANIMAL,
    format: ImageFormat = ImageFormat.SVG
): Image = ImageUrlMapper.getAnimalCardImageUrl(color, animal, format)

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
    val description: String,
    val englishName: String
) {
    GREEN("초록", "Green"),
    RED("붉은", "Red"),
    YELLOW("노란", "Yellow"),
    WHITE("흰", "White"),
    BLACK("검정", "Black")
}

enum class ANIMAL(
    val description: String,
    val englishName: String
) {
    MOUSE("쥐", "Mouse"),
    COW("소", "Meet"),
    TIGER("호랑이", "Tiger"),
    RABBIT("토끼", "Rabbit"),
    DRAGON("용", "Dragon"),
    SNAKE("뱀", "Snake"),
    HORSE("말", "Horse"),
    SHEEP("양", "Sheep"),
    MONKEY("원숭이", "Monkey"),
    ROOSTER("닭", "Chicken"),
    DOG("개", "Dog"),
    PIG("돼지", "Pig")
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

        val weeklySpendMoney =
            weeklyPastRoutines
                .map { it.missions.calculateSpendMoney() }
                .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

        val monthlyPastRoutines = userService.getMonthlyPastRoutines(userId = userId, date = baseYearMonth)

        val monthlySpendMoney =
            monthlyPastRoutines
                .map { it.missions.calculateSpendMoney() }
                .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

        call.respond(
            HttpStatusCode.OK,
            AnalysisViewResponse(
                totalSavedMoney = user.totalSavedMoney,
                weeklySavedMoney = weeklySavedMoney,
                weeklySpendMoney = weeklySpendMoney,
                monthlySpendMoney = monthlySpendMoney,
                generationMoneyViewResponse =
                    GenerationMoneyViewResponse(
                        gender = user.gender,
                        generation = user.generation.description,
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
                                it.missions.size
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

                val days =
                    if (currentMonthLocalDate.month == now.month) {
                        now.dayOfMonth
                    } else {
                        val isLeapYear = currentMonthLocalDate.isLeapYear()
                        currentMonthLocalDate.month.length(isLeapYear)
                    }

                val averageSucceedProbability: Int = monthlyFinishedPercentage / days

                AnalysisMonthViewResponse(
                    month = currentMonthLocalDate.month.value,
                    averageSucceedProbability = averageSucceedProbability,
                    calendarDayMissionViews = calendarDayMissionViews
                )
            }

        call.respond(HttpStatusCode.OK, CalendarViewResponse(threeMonthViewResponse))
    }
}

private fun Route.getDailyFortune(fortuneService: FortuneService) {
    get("/{userId}/fortune") {
        val userId =
            call.pathParameters["userId"]
                ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        val requestDate: LocalDate =
            call.request.queryParameters["date"]
                ?.let { dateStr -> LocalDate.parse(dateStr) } ?: now()

        val formatParam = call.request.queryParameters["format"] ?: "svg"
        val format =
            try {
                ImageFormat.valueOf(formatParam.uppercase())
            } catch (e: IllegalArgumentException) {
                ImageFormat.SVG
            }

        call.respond(
            HttpStatusCode.OK,
            FortuneResponse.from(
                fortuneService.queryDailyFortune(userId, requestDate),
                format
            )
        )
    }
}

private fun Route.addJulyPastRoutineHistory(userService: UserService) {
    post("/{userId}/add-july-history") {
        val userId = call.pathParameters["userId"] ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

        val currentYear = now().year
        val july = 7

        val user = userService.getUserById(userId)

        // 7월 데이터를 추가할 날짜들과 완료 상태
        val julyData =
            mapOf(
                1 to 3, // 1일: 3개 모두 완료
                2 to 3, // 2일: 3개 모두 완료
                3 to 3, // 3일: 3개 모두 완료
                4 to 1, // 4일: 1개 완료
                7 to 3, // 7일: 3개 모두 완료
                8 to 2, // 8일: 2개 완료
                11 to 3, // 11일: 3개 모두 완료
                14 to 3, // 14일: 3개 모두 완료
                15 to 3, // 15일: 3개 모두 완료
                17 to 3, // 17일: 3개 모두 완료
                18 to 3 // 18일: 3개 모두 완료
            )

        val addedHistories = userService.addJulyPastRoutineHistories(userId, currentYear, julyData)

        call.respond(
            HttpStatusCode.Created,
            AddJulyHistoryResponse(
                userId = userId,
                year = currentYear,
                month = july,
                addedDays = addedHistories.size,
                totalSavedMoney =
                    addedHistories
                        .flatMap { it.missions }
                        .filter { it.finished }
                        .map { it.cost }
                        .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)
            )
        )
    }
}