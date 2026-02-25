package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.FortuneCard
import com.mashup.dhc.domain.model.FortuneScoreRange
import com.mashup.dhc.domain.model.FortuneTip
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Generation
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.calculatePoint
import com.mashup.dhc.domain.model.calculateSavedMoney
import com.mashup.dhc.domain.model.calculateSpendMoney
import com.mashup.dhc.domain.service.FortuneService
import com.mashup.dhc.domain.service.GeminiService
import com.mashup.dhc.domain.service.LoveMissionService
import com.mashup.dhc.domain.service.PointMultiplierService
import com.mashup.dhc.domain.service.ShareService
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
import io.ktor.server.routing.RoutingCall
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
import org.bson.types.ObjectId

// =============================================================================
// Helper Functions
// =============================================================================

private fun RoutingCall.requirePathParameter(name: String): String =
    pathParameters[name] ?: throw BusinessException(ErrorCode.INVALID_REQUEST)

private fun parseImageFormat(formatParam: String?): ImageFormat =
    try {
        ImageFormat.valueOf((formatParam ?: "svg").uppercase())
    } catch (e: IllegalArgumentException) {
        ImageFormat.SVG
    }

private fun List<Money>.sumOrZero(): Money =
    reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

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
    fortuneService: FortuneService,
    shareService: ShareService,
    loveMissionService: LoveMissionService,
    pointMultiplierService: PointMultiplierService,
    geminiService: GeminiService
) {
    route("/api/users") {
        register(userService)
        changeMissionStatus(userService, loveMissionService)
        endToday(userService, pointMultiplierService)
        logout(userService)
        searchUser(userService)
        getDailyFortune(fortuneService)
        addJulyPastRoutineHistory(userService)
        createShareCode(shareService)
        yearlyFortune(userService, geminiService)
    }
    route("/view/users/{userId}") {
        home(userService, fortuneService, loveMissionService, pointMultiplierService)
        myPage(userService)
        analysisView(userService)
        calendarView(userService)
        rewardProgress(userService)
        getYearlyFortune(userService)
    }
    route("/api") {
        missionCategoriesRoutes()
        loveTest(geminiService)
    }
    route("/api/share") {
        completeShare(shareService)
    }
    route("/api") {
        qaRoutes(userService)
    }
}

fun Route.loveTest(geminiService: GeminiService) {
    post("/love-test") {
        val request = call.receive<LoveTestRequest>()
        val geminiResponse = geminiService.requestLoveTest(request)

        val score = geminiResponse.score
        val scoreRange = FortuneScoreRange.fromScore(score)

        call.respond(
            HttpStatusCode.OK,
            LoveTestViewResponse(
                score = score,
                fortuneDetail = geminiResponse.fortuneDetail,
                fortuneCard = FortuneCard(
                    image = scoreRange.getCardImage(),
                    title = scoreRange.animalName,
                    subTitle = scoreRange.title
                ),
                fortuneTips = listOf(
                    FortuneTip(
                        image = ImageUrlMapper.Fortune.getTodayMenuImageUrl(ImageFormat.SVG),
                        title = "행운의 메뉴",
                        description = geminiResponse.todayMenu
                    ),
                    FortuneTip(
                        image = ImageUrlMapper.Fortune.getLuckyColorImageUrl(ImageFormat.SVG),
                        title = "행운의 색상",
                        description = geminiResponse.luckyColor,
                        hexColor = geminiResponse.luckyColorHex
                    ),
                    FortuneTip(
                        image = ImageUrlMapper.Fortune.getJinxedColorImageUrl(ImageFormat.SVG),
                        title = "피해야 할 색상",
                        description = geminiResponse.jinxedColor,
                        hexColor = geminiResponse.jinxedColorHex
                    ),
                    FortuneTip(
                        image = ImageUrlMapper.Fortune.getJinxedMenuImageUrl(ImageFormat.SVG),
                        title = "이 음식은 조심해!",
                        description = geminiResponse.jinxedMenu
                    )
                ),
                confessDate = LocalDate.parse(geminiResponse.confessDate),
                confessLocation = geminiResponse.confessLocation
            )
        )
    }
}

fun Route.rewardProgress(userService: UserService) {
    get("/reward-progress") {
        val userId = call.requirePathParameter("userId")
        val user = userService.getUserById(userId)
        val totalPoint = user.point

        val currentLevel = RewardUserResponse.RewardLevel.fromTotalPoint(totalPoint)
        val nextLevel = RewardUserResponse.RewardLevel.getNextLevel(currentLevel)

        // 현재 레벨 내에서의 포인트 (현재 레벨 시작점부터)
        val currentLevelPoint = totalPoint - currentLevel.requiredTotalPoint

        // 다음 레벨까지 필요한 포인트 (다음 레벨 시작점 - 현재 레벨 시작점)
        val nextLevelRequiredPoint = nextLevel?.let { it.requiredTotalPoint - currentLevel.requiredTotalPoint }

        val isYearlyFortuneUnlocked = currentLevel.level >= 8

        call.respond(
            HttpStatusCode.OK,
            RewardProgressViewResponse(
                RewardUserResponse(
                    rewardImageUrl = ImageUrlMapper.getRewardLevelImageUrl(currentLevel.level),
                    rewardLevel = currentLevel.toInfo(),
                    totalPoint = totalPoint,
                    currentLevelPoint = currentLevelPoint,
                    nextLevelRequiredPoint = nextLevelRequiredPoint
                ),
                listOf(
                    RewardItemResponse(
                        id = 1,
                        title = "1년 운세",
                        isUnlocked = isYearlyFortuneUnlocked,
                        isUsed = user.yearlyFortuneUsed,
                        iconURL = if (isYearlyFortuneUnlocked) ImageUrlMapper.getRewardLevelImageUrl(8) else null,
                        message = when {
                            !isYearlyFortuneUnlocked -> "레벨 8 달성 시 해금됩니다"
                            !user.yearlyFortuneUsed -> "1년 운세를 확인해보세요!"
                            else -> null
                        },
                        type = RewardType.YEARLY_FORTUNE
                    )
                )
            )
        )
    }
}

fun Route.yearlyFortune(
    userService: UserService,
    geminiService: GeminiService
) {
    post("/{userId}/yearly-fortune") {
        val userId = call.requirePathParameter("userId")
        val user = userService.getUserById(userId)
        val totalPoint = user.point
        val currentLevel = RewardUserResponse.RewardLevel.fromTotalPoint(totalPoint)

        // 레벨 8 이상 체크
        if (currentLevel.level < 8) {
            throw BusinessException(ErrorCode.LEVEL_NOT_ENOUGH)
        }

        // 이미 사용한 경우 체크
        if (user.yearlyFortuneUsed) {
            throw BusinessException(ErrorCode.YEARLY_FORTUNE_ALREADY_USED)
        }

        // Gemini API 호출
        val request = user.toGeminiFortuneRequest()
        val yearlyFortune = geminiService.requestYearlyFortune(request)

        // User에 저장 (yearlyFortuneUsed도 true로 변경)
        userService.updateYearlyFortune(ObjectId(userId), yearlyFortune)

        call.respond(HttpStatusCode.Created, CreateYearlyFortuneResponse(success = true))
    }
}

fun Route.getYearlyFortune(userService: UserService) {
    get("/yearly-fortune") {
        val userId = call.requirePathParameter("userId")
        val user = userService.getUserById(userId)

        // 1년 운세가 없으면 에러
        val yearlyFortune =
            user.yearlyFortune
                ?: throw BusinessException(ErrorCode.YEARLY_FORTUNE_NOT_CREATED)

        call.respond(HttpStatusCode.OK, YearlyFortuneResponse.from(yearlyFortune))
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
        val format = parseImageFormat(call.request.queryParameters["format"])
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
    fortuneService: FortuneService,
    loveMissionService: LoveMissionService,
    pointMultiplierService: PointMultiplierService
) {
    get("/home") {
        val userId = call.requirePathParameter("userId")

        var user = userService.getUserById(userId)

        val now =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        // 2일 이상 미접속 여부 및 첫 접속 여부 계산 (lastAccessDate 업데이트 전에 계산)
        val lastAccess = user.lastAccessDate
        val createdDate = user.id?.let {
            Instant.fromEpochSeconds(it.timestamp.toLong())
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        val isFirstAccess = user.qaOverrideIsFirstAccess ?: (createdDate == now)
        val daysSinceLastAccess = lastAccess?.let { (now.toEpochDays() - it.toEpochDays()).toInt() } ?: 0
        val longAbsence = user.qaOverrideLongAbsence ?: (daysSinceLastAccess >= 2)

        // lastAccessDate 업데이트
        userService.updateLastAccessDate(userId, now)

        // summaryTodayMission이 어제 날짜로 PastRoutineHistory를 생성할 수 있으므로 먼저 실행
        val isAlreadyAllDone = user.todayDailyMissionList.any { it.endDate?.run { this < now } ?: false }
        if (isAlreadyAllDone) {
            userService.summaryTodayMission(
                userId,
                user.todayDailyMissionList.random().endDate!!
            )
            user = userService.getUserById(userId) // 유저 정보 갱신
        }

        // summaryTodayMission 이후에 조회해야 정확한 기록을 얻을 수 있음
        val todayPastRoutines = userService.getTodayPastRoutines(userId, now)
        val yesterdayPastRoutines = userService.getYesterdayPastRoutines(userId, now)

        // 어제 일일 미션 성공 여부 (DAILY 타입만 체크)
        val yesterdayMissionSuccess =
            user.qaOverrideYesterdayMissionSuccess
                ?: yesterdayPastRoutines
                    .flatMap { it.missions }
                    .filter { it.type == MissionType.DAILY }
                    .any { it.finished }

        // 어제 획득한 포인트 계산 (배율 적용)
        val basePoint =
            yesterdayPastRoutines
                .flatMap { it.missions }
                .filter { it.finished }
                .sumOf { it.calculatePoint() }
        val multiplier = pointMultiplierService.calculateMultiplier(user, now, yesterdayPastRoutines)
        val yesterdayEarnedPoint = basePoint * multiplier

        val todayDailyFortune = fortuneService.queryDailyFortune(userId, now)

        if (user.dailyFortunes == null || user.dailyFortunes.all { LocalDate.parse(it.date) < now }) {
            fortuneService.enqueueGenerateDailyFortuneTask(
                user.id.toString(),
                user.toGeminiFortuneRequest()
            )
        }

        // 러브미션 확인 및 활성화 (완료된 공유가 있으면 자동 활성화)
        val loveMissionInfo = loveMissionService.checkAndGetTodayLoveMission(user, now)

        // 포인트 적립 후 최신 user 정보 다시 가져오기
        val latestUser = userService.getUserById(userId)

        val todayDone = user.qaOverrideTodayDone ?: todayPastRoutines.isNotEmpty()

        // todayDailyMissionList 구성
        // todayDone이면 완료한 미션(PastRoutineHistory)을 반환, 아니면 현재 미션 반환
        val todayMissions = if (todayDone) {
            todayPastRoutines
                .flatMap { it.missions }
                .map { MissionResponse.from(it) }
        } else {
            val dailyMissions = latestUser.todayDailyMissionList.map { MissionResponse.from(it) }
            if (loveMissionInfo != null) {
                val loveMissionResponse =
                    MissionResponse.fromLoveMission(
                        loveMissionInfo.mission,
                        loveMissionInfo.dayNumber,
                        loveMissionInfo.remainingDays
                    )
                listOf(loveMissionResponse) + dailyMissions
            } else {
                dailyMissions
            }
        }

        // 궁합 테스트 배너 (목 데이터)
        val testBanner =
            TestBannerResponse(
                version = 1,
                title = "궁합 테스트에 참여하고\n스페셜 미션 받아보세요",
                subTitle = "지금까지 389명이 참여했어요!",
                imageUrl = null,
                testUrl = null
            )

        call.respond(
            HttpStatusCode.OK,
            HomeViewResponse(
                longTermMission = if (todayDone) {
                    todayPastRoutines
                        .flatMap { it.missions }
                        .firstOrNull { it.type == MissionType.LONG_TERM }
                        ?.let { MissionResponse.from(it) }
                } else {
                    latestUser.longTermMission?.let { MissionResponse.from(it) }
                },
                todayDailyMissionList = todayMissions,
                todayDailyFortune = todayDailyFortune.let { FortuneResponse.from(it) },
                todayDone = todayDone,
                yesterdayMissionSuccess = yesterdayMissionSuccess,
                longAbsence = longAbsence,
                isFirstAccess = isFirstAccess,
                point = yesterdayEarnedPoint,
                testBanner = testBanner
            )
        )
    }
}

private fun Route.endToday(
    userService: UserService,
    pointMultiplierService: PointMultiplierService
) {
    post("/{userId}/done") {
        val userId = call.requirePathParameter("userId")
        val request = call.receive<EndTodayMissionRequest>()

        request.validate()

        // 미션 정보 먼저 가져오기 (summaryTodayMission 호출 전)
        val user = userService.getUserById(userId)
        val todayMissions = user.todayDailyMissionList

        // 미션 성공 여부 (하나라도 완료했으면 성공)
        val missionSuccess = todayMissions.any { it.finished }

        // 어제 기록 조회 (배율 계산용)
        val today = now()
        val yesterdayHistory = userService.getYesterdayPastRoutines(userId, today)

        // 배율 계산
        val multiplier = pointMultiplierService.calculateMultiplier(user, today, yesterdayHistory)

        // 획득 포인트 계산 (배율 적용)
        val basePoint =
            todayMissions
                .filter { it.finished }
                .sumOf { it.calculatePoint() }
        val earnedPoint = basePoint * multiplier

        val todaySavedMoney =
            userService.summaryTodayMission(
                userId,
                request.date
            )

        call.respond(
            HttpStatusCode.OK,
            EndTodayMissionResponse(
                todaySavedMoney = todaySavedMoney,
                missionSuccess = missionSuccess,
                earnedPoint = earnedPoint
            )
        )
    }
}

private fun Route.changeMissionStatus(
    userService: UserService,
    loveMissionService: LoveMissionService
) {
    put("/{userId}/missions/{missionId}") {
        val userId = call.requirePathParameter("userId")
        val missionId = call.requirePathParameter("missionId")
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

        // 러브미션인지 확인
        val user = userService.getUserById(userId)
        val loveMission = user.loveMissionStatus?.findMissionById(missionId)
        val isLoveMission = loveMission != null

        // 일반 미션 찾기 (포인트 계산용)
        val regularMission =
            (user.todayDailyMissionList + user.longTermMission)
                .filterNotNull()
                .find { it.id.toString() == missionId }

        val updated =
            if (isLoveMission) {
                // 러브미션은 완료만 가능 (switch 불가)
                if (request.finished != null) {
                    loveMissionService.updateLoveMissionFinished(
                        ObjectId(userId),
                        missionId,
                        request.finished
                    ) ?: throw BusinessException(ErrorCode.INVALID_REQUEST)
                } else {
                    // 러브미션은 switch 불가
                    throw BusinessException(ErrorCode.INVALID_REQUEST)
                }
            } else {
                // 일반 미션
                if (request.finished != null) {
                    userService.updateTodayMission(userId, missionId, request.finished)
                } else {
                    userService.switchTodayMission(userId, missionId)
                }
            }

        // 미션 완료 시 포인트 적립 (finished = true이고, 이전에 완료되지 않았던 경우)
        if (request.finished == true) {
            val mission = loveMission ?: regularMission
            if (mission != null && !mission.finished) {
                val pointToAdd = mission.calculatePoint()
                userService.addPoint(userId, pointToAdd)
            }
        }

        val missions = updated.todayDailyMissionList + listOfNotNull(updated.longTermMission)
        call.respond(HttpStatusCode.OK, ToggleMissionResponse(missions.map { MissionResponse.from(it) }))
    }
}

private fun Route.myPage(userService: UserService) {
    get("/myPage") {
        val userId = call.requirePathParameter("userId")
        val user = userService.getUserById(userId)
        val format = parseImageFormat(call.request.queryParameters["format"])

        call.respond(
            HttpStatusCode.OK,
            MyPageResponse(
                user.resolveAnimalCard(format),
                user.birthDate,
                user.birthTime,
                user.preferredMissionCategoryList.map { MissionCategoryResponse.from(it, format) },
                true, // TODO: alarm
                fortuneTests = listOf(
                    FortuneTestInfo(
                        imageURL = null,
                        displayName = "궁합 테스트",
                        testURL = null
                    )
                )
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
        val userId = call.requirePathParameter("userId")
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
        val userId = call.requirePathParameter("userId")
        val user = userService.getUserById(userId)
        val now = now()
        val baseYearMonth = LocalDate(now.year, now.month, 1)

        val weeklyPastRoutines = userService.getWeekPastRoutines(userId = userId, date = baseYearMonth)
        val weeklySavedMoney = weeklyPastRoutines.map { it.missions.calculateSavedMoney() }.sumOrZero()
        val weeklySpendMoney = weeklyPastRoutines.map { it.missions.calculateSpendMoney() }.sumOrZero()

        val monthlyPastRoutines = userService.getMonthlyPastRoutines(userId = userId, date = baseYearMonth)
        val monthlySpendMoney = monthlyPastRoutines.map { it.missions.calculateSpendMoney() }.sumOrZero()

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
        val userId = call.requirePathParameter("userId")

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
        val userId = call.requirePathParameter("userId")
        val requestDate = call.request.queryParameters["date"]?.let { LocalDate.parse(it) } ?: now()
        val format = parseImageFormat(call.request.queryParameters["format"])

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
        val userId = call.requirePathParameter("userId")
        val currentYear = now().year
        val july = 7

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

        val totalSavedMoney = addedHistories
            .flatMap { it.missions }
            .filter { it.finished }
            .map { it.cost }
            .sumOrZero()

        call.respond(
            HttpStatusCode.Created,
            AddJulyHistoryResponse(
                userId = userId,
                year = currentYear,
                month = july,
                addedDays = addedHistories.size,
                totalSavedMoney = totalSavedMoney
            )
        )
    }
}

private fun Route.createShareCode(shareService: ShareService) {
    post("/{userId}/share") {
        val userId = call.requirePathParameter("userId")
        val result = shareService.createShareCode(userId)

        call.respond(
            HttpStatusCode.Created,
            CreateShareCodeResponse(
                shareCode = result.shareCode
            )
        )
    }
}

private fun Route.completeShare(shareService: ShareService) {
    post("/{shareCode}/complete") {
        val shareCode = call.requirePathParameter("shareCode")
        val result = shareService.completeShare(shareCode)

        call.respond(
            HttpStatusCode.OK,
            CompleteShareResponse(
                shareCode = result.shareCode,
                alreadyCompleted = result.alreadyCompleted
            )
        )
    }
}

// =============================================================================
// QA Routes
// =============================================================================

fun Route.qaRoutes(userService: UserService) {
    route("/qa/users/{userId}") {
        put("/home-state") {
            val userId = call.requirePathParameter("userId")
            userService.getUserById(userId) // 존재 확인
            val request = call.receive<QaHomeStateRequest>()

            userService.updateQaHomeStateOverrides(
                userId,
                request.longAbsence,
                request.yesterdayMissionSuccess,
                request.todayDone,
                request.isFirstAccess
            )

            call.respond(HttpStatusCode.OK, QaSuccessResponse(success = true))
        }

        put("/point") {
            val userId = call.requirePathParameter("userId")
            userService.getUserById(userId) // 존재 확인
            val request = call.receive<QaPointRequest>()

            userService.setPoint(userId, request.point)
            val level = RewardUserResponse.RewardLevel.fromTotalPoint(request.point)

            call.respond(
                HttpStatusCode.OK,
                QaPointResponse(
                    success = true,
                    point = request.point,
                    level = level.level
                )
            )
        }

        delete("/yearly-fortune") {
            val userId = call.requirePathParameter("userId")
            userService.getUserById(userId) // 존재 확인

            userService.resetYearlyFortune(userId)

            call.respond(HttpStatusCode.OK, QaSuccessResponse(success = true))
        }
    }
}