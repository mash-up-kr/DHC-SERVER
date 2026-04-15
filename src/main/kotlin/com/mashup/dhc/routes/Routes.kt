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
        wealthTestRoutes()
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
                    image = ImageUrlMapper.getRewardLevelImageUrl(currentLevel.level),
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
                        icon = if (isYearlyFortuneUnlocked) ImageUrlMapper.getRewardLevelImageUrl(8) else null,
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
            // 미션 수행일 = endDate - 1 (일일 미션은 생성일 + 1이 endDate)
            val missionDate = user.todayDailyMissionList.first().endDate!!
                .minus(1, DateTimeUnit.DAY)

            // 자동 롤오버: 미션 수행일 기준으로 포인트 정산 (배율 적용)
            val allMissions = user.todayDailyMissionList + listOfNotNull(user.longTermMission)
            settleMissions(
                userId, missionDate, allMissions, userService, pointMultiplierService, isExplicit = false
            )

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

        // 어제 획득한 포인트 계산 (배율 적용) - 표시용
        val basePoint =
            yesterdayPastRoutines
                .flatMap { it.missions }
                .filter { it.finished }
                .sumOf { it.calculatePoint() }
        val latestHistoryDate = userService.getLatestPastRoutineDate(userId)
        val multiplier = pointMultiplierService.calculateMultiplier(now, yesterdayPastRoutines, latestHistoryDate)
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
                .filter { it.type != MissionType.LONG_TERM }
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
                image = Image.custom("logos/icon/test_title_modal.png"),
                imageUrl = Image.custom("logos/icon/test_title_modal.png"),
                testUrl = "https://dhc-web.vercel.app/love-test"
            )

        call.respond(
            HttpStatusCode.OK,
            HomeViewResponse(
                longTermMission = if (todayDone) {
                    // PastRoutineHistory에 longTermMission이 있으면 사용, 없으면 현재 유저의 longTermMission 사용
                    (todayPastRoutines
                        .flatMap { it.missions }
                        .firstOrNull { it.type == MissionType.LONG_TERM }
                        ?: latestUser.longTermMission)
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
        val allMissions = user.todayDailyMissionList + listOfNotNull(user.longTermMission)

        // 미션 성공 여부 (하나라도 완료했으면 성공)
        val missionSuccess = allMissions.any { it.finished }

        val today = now()

        // 포인트 정산 (배율 적용)
        val earnedPoint = settleMissions(
            userId, today, allMissions, userService, pointMultiplierService, isExplicit = true
        )

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
                        imageURL = "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/axircf8nexkb/b/dhc-storage/o/logos/icon/love_test.png",
                        image = Image.custom("logos/icon/love_test.png"),
                        displayName = "궁합 테스트",
                        testURL = "https://dhc-web.vercel.app/love-test",
                        testUrl = "https://dhc-web.vercel.app/love-test"
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
// 부자 테스트 (Wealth Fortune Test) - Mock
// =============================================================================

private val wealthFortuneResults = listOf(
    WealthFortuneResultResponse(
        id = 1,
        fortuneType = "마이더스의 손 형",
        fortuneTypeDescription = "손만 대면 수익이 터지는 사업가",
        fortuneDetail = "당신은 자본주의 시장에서 돈이 흐르는 길목을 본능적으로 포착하는 사냥꾼의 감각을 타고났습니다. 남들이 위험하다고 고개를 저을 때 당신은 그 안의 숨겨진 가치를 발견하며, 과감한 결단력으로 기회를 수익으로 치환하는 에너지가 강력합니다.\n\n특히 위기 상황에서 평정심을 유지하며 남들이 보지 못하는 틈새를 공략하는 기질은 당신을 최상위 자산가로 이끄는 핵심 동력이 됩니다. 하이 리스크 종목에서도 당신만의 탈출 타이밍을 정확히 짚어내어 손실은 최소화하고 이익은 극대화하는 천부적인 감각을 지녔습니다.\n\n말년에는 직접 움직이지 않아도 자본이 스스로 일하는 완벽한 현금 흐름 시스템을 소유하게 될 것입니다. 다만 너무 이른 성공으로 인해 주변의 시기가 따를 수 있으니, 적절한 시기에 나눔을 실천한다면 그 부의 유통기한은 대대로 이어질 것입니다.",
        fortuneTypeImageUrl = Image.custom("logos/fortune/wealth/midas.png"),
        graphData = listOf(
            WealthFortuneGraphPoint(age = 20, amount = 55_000_000),
            WealthFortuneGraphPoint(age = 30, amount = 300_000_000),
            WealthFortuneGraphPoint(age = 40, amount = 800_000_000),
            WealthFortuneGraphPoint(age = 50, amount = 1_800_000_000),
            WealthFortuneGraphPoint(age = 60, amount = 3_500_000_000),
            WealthFortuneGraphPoint(age = 70, amount = 6_000_000_000),
            WealthFortuneGraphPoint(age = 80, amount = 9_000_000_000)
        ),
        events = listOf(
            WealthFortuneEvent(age = 31, description = "대충 던진 밈코인이 떡상함", amount = 120_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png")),
            WealthFortuneEvent(age = 46, description = "술김에 산 골동품이 가짜로 판명", amount = -45_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_negative.png")),
            WealthFortuneEvent(age = 62, description = "취미로 만든 앱이 대기업에 팔림", amount = 2_000_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png"))
        )
    ),
    WealthFortuneResultResponse(
        id = 2,
        fortuneType = "마이너스의 손 형",
        fortuneTypeDescription = "내가 사면 떨어지고 팔면 오르는 기이한 운명",
        fortuneDetail = "당신의 직관은 이상하리만큼 시장의 단기적인 파동과 정반대로 움직이는 경향이 있습니다. 매수를 결정한 직후 차트가 꺾이거나, 인내하다 팔아치운 직후 폭등하는 경험을 반복하며 '나는 재물복이 없다'고 생각하기 쉽지만 이는 사실이 아닙니다.\n\n당신의 진짜 재물복은 '직관'이 아닌 '무심함'과 '시간'에 설계되어 있습니다. 스스로의 판단을 믿기보다는 우량한 자산에 장기적으로 묻어두는 전략이 필요합니다. 잔파도에 흔들리지 않고 묵묵히 숫자를 모아가는 과정에서 당신의 운명적인 꼬임이 풀리기 시작합니다.\n\n복리의 마법이 당신의 모든 실수를 상쇄하고도 남을 만큼 거대한 성벽을 쌓아줄 것입니다. 말년에는 누구보다 단단하고 평온한 경제적 자유를 누릴 반전의 주인공이 될 것이니, 당장의 수익률에 일희일비하지 않는 태도가 부자로 가는 유일한 길입니다.",
        fortuneTypeImageUrl = Image.custom("logos/fortune/wealth/minus.png"),
        graphData = listOf(
            WealthFortuneGraphPoint(age = 20, amount = 40_000_000),
            WealthFortuneGraphPoint(age = 30, amount = 30_000_000),
            WealthFortuneGraphPoint(age = 40, amount = 25_000_000),
            WealthFortuneGraphPoint(age = 50, amount = 60_000_000),
            WealthFortuneGraphPoint(age = 60, amount = 150_000_000),
            WealthFortuneGraphPoint(age = 70, amount = 120_000_000),
            WealthFortuneGraphPoint(age = 80, amount = 90_000_000)
        ),
        events = listOf(
            WealthFortuneEvent(age = 29, description = "풀매수하자마자 거래정지 당함", amount = -30_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_negative.png")),
            WealthFortuneEvent(age = 50, description = "중고거래 사기로 벽돌 배송받음", amount = -1_500_000, iconUrl = Image.custom("logos/fortune/wealth/event_negative.png")),
            WealthFortuneEvent(age = 73, description = "30년 전 잊었던 주식이 대박남", amount = 500_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png"))
        )
    ),
    WealthFortuneResultResponse(
        id = 3,
        fortuneType = "짠테크 만렙 고수형",
        fortuneTypeDescription = "티끌 모아 빌딩 올릴 자린고비",
        fortuneDetail = "당신은 지출의 누수를 허용하지 않는 금융 성벽의 파수꾼입니다. 100원 단위의 지출조차 효율성을 따지며, 불필요한 과시 소비보다는 자산이 늘어가는 숫자의 즐거움을 누구보다 잘 알고 있습니다. 남들이 욜로를 외칠 때 당신은 미래를 설계하는 냉철한 현실주의자입니다.\n\n젊은 시절 남들이 유행을 쫓을 때 당신이 다진 비옥한 종잣돈은 노년에 거대한 그늘과 끊이지 않는 열매를 선사할 것입니다. 절약은 단순히 아끼는 행위가 아니라, 인생의 통제권을 본인의 손에 쥐는 과정임을 당신은 이미 증명하고 있습니다.\n\n60대 이후에는 친구들 사이에서 가장 먼저 '조물주 위의 건물주' 타이틀을 거머쥐게 될 실속파 부자의 전형입니다. 검소함 속에 숨겨진 강력한 자본력은 위기 상황에서 더욱 빛을 발하며, 당신을 가장 안전한 상류층으로 이끌어 줄 것입니다.",
        fortuneTypeImageUrl = Image.custom("logos/fortune/wealth/saver.png"),
        graphData = listOf(
            WealthFortuneGraphPoint(age = 20, amount = 60_000_000),
            WealthFortuneGraphPoint(age = 30, amount = 180_000_000),
            WealthFortuneGraphPoint(age = 40, amount = 400_000_000),
            WealthFortuneGraphPoint(age = 50, amount = 900_000_000),
            WealthFortuneGraphPoint(age = 60, amount = 1_500_000_000),
            WealthFortuneGraphPoint(age = 70, amount = 2_500_000_000),
            WealthFortuneGraphPoint(age = 80, amount = 3_500_000_000)
        ),
        events = listOf(
            WealthFortuneEvent(age = 26, description = "길가다 5만원권 줍기 성공", amount = 50_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png")),
            WealthFortuneEvent(age = 44, description = "경품 응모로 냉장고 당첨됨", amount = 3_500_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png")),
            WealthFortuneEvent(age = 71, description = "30년 모은 포인트로 명품 가방 삼", amount = 10_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png"))
        )
    ),
    WealthFortuneResultResponse(
        id = 4,
        fortuneType = "금사빠 금전 투자형",
        fortuneTypeDescription = "감정이 앞서는 로맨티스트 투자자",
        fortuneDetail = "당신은 투자 대상과 쉽게 사랑에 빠지는 로맨틱한 투자자입니다. 매혹적인 수익률 광고나 지인의 확신에 찬 한마디에 심장이 먼저 뛰며, 논리적인 분석보다는 미래의 장밋빛 전망에 현혹되기 쉬운 기질을 가지고 있습니다. 한 번 믿음을 주면 끝까지 믿어버리는 순수함이 장점이자 단점입니다.\n\n신기하게도 절체절명의 위기마다 당신을 돕는 귀인이 나타나거나 예상치 못한 횡재수로 구사일생하는 천운을 타고났습니다. 큰 손실을 보고도 금방 기운을 차려 다시 도전하는 긍정적인 에너지는 재물운을 끌어당기는 자석 역할을 합니다.\n\n이성적인 파트너의 조언을 시스템화한다면, 당신의 도전 정신은 남들이 상상하지 못한 기적 같은 자산 규모를 현실로 만들어줄 것입니다. 말년에는 풍부한 경험을 바탕으로 투자계의 마당발로 통하며 남부럽지 않은 자산을 유지하게 됩니다.",
        fortuneTypeImageUrl = Image.custom("logos/fortune/wealth/romantic.png"),
        graphData = listOf(
            WealthFortuneGraphPoint(age = 20, amount = 35_000_000),
            WealthFortuneGraphPoint(age = 30, amount = 50_000_000),
            WealthFortuneGraphPoint(age = 40, amount = 70_000_000),
            WealthFortuneGraphPoint(age = 50, amount = 55_000_000),
            WealthFortuneGraphPoint(age = 60, amount = 40_000_000),
            WealthFortuneGraphPoint(age = 70, amount = 180_000_000),
            WealthFortuneGraphPoint(age = 80, amount = 400_000_000)
        ),
        events = listOf(
            WealthFortuneEvent(age = 32, description = "사기꾼에게 속아 '우주 땅' 매입", amount = -20_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_negative.png")),
            WealthFortuneEvent(age = 51, description = "가짜 연예인 사칭에 후원금 보냄", amount = -50_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_negative.png")),
            WealthFortuneEvent(age = 69, description = "창고에서 레어 카드 뭉치 발견", amount = 100_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png"))
        )
    ),
    WealthFortuneResultResponse(
        id = 5,
        fortuneType = "뚝심 있는 외길 장인형",
        fortuneTypeDescription = "한 우물만 파서 부를 일구는 실력파",
        fortuneDetail = "유행하는 테마주나 코인 열풍 속에서도 당신은 본인이 가야 할 길을 묵묵히 걷는 사람입니다. 당장의 큰 이익보다 본인의 전문 기술이나 커리어를 갈고닦는 데 집중하며, 그렇게 쌓인 '전문성'은 시간이 지남에 따라 대체 불가능한 가치로 치환됩니다.\n\n초반에는 성장이 더뎌 보여 조바심이 날 수도 있으나, 임계점을 돌파하는 순간 당신의 부는 폭발적인 가속도를 얻게 됩니다. 정직하게 흘린 땀방울이 자본으로 치환되는 과정은 누구보다 견고하며, 경기 불황의 파도에도 흔들리지 않는 뿌리 깊은 재력을 형성하게 합니다.\n\n말년에는 해당 분야의 거장으로서 부와 명예를 동시에 거머쥐고 자손들에게도 존경받는 자산가가 될 것입니다. 당신의 성실함은 배신하지 않으며, 인생 후반전으로 갈수록 금전적 여유와 정신적 만족감이 동시에 정점에 달하게 됩니다.",
        fortuneTypeImageUrl = Image.custom("logos/fortune/wealth/craftsman.png"),
        graphData = listOf(
            WealthFortuneGraphPoint(age = 20, amount = 25_000_000),
            WealthFortuneGraphPoint(age = 30, amount = 90_000_000),
            WealthFortuneGraphPoint(age = 40, amount = 200_000_000),
            WealthFortuneGraphPoint(age = 50, amount = 600_000_000),
            WealthFortuneGraphPoint(age = 60, amount = 1_200_000_000),
            WealthFortuneGraphPoint(age = 70, amount = 2_200_000_000),
            WealthFortuneGraphPoint(age = 80, amount = 3_000_000_000)
        ),
        events = listOf(
            WealthFortuneEvent(age = 38, description = "한 우물 판 기술로 연봉 떡상", amount = 80_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png")),
            WealthFortuneEvent(age = 55, description = "장인 정신에 감동한 부자가 땅 증여", amount = 500_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png")),
            WealthFortuneEvent(age = 74, description = "평생의 노하우 담긴 저서가 대박", amount = 200_000_000, iconUrl = Image.custom("logos/fortune/wealth/event_positive.png"))
        )
    )
)

// Mock 인메모리 저장소
private data class StoredWealthResult(
    val resultId: String,
    val name: String,
    val result: WealthFortuneResultResponse,
    val createdAt: Long = System.currentTimeMillis()
)

private data class StoredWealthGroup(
    val groupId: String,
    val groupName: String,
    val inviteCode: String,
    val memberResultIds: MutableList<String>
)

private val wealthResultStore = java.util.concurrent.ConcurrentHashMap<String, StoredWealthResult>()
private val wealthGroupStore = java.util.concurrent.ConcurrentHashMap<String, StoredWealthGroup>()
private val wealthInviteCodeIndex = java.util.concurrent.ConcurrentHashMap<String, String>() // inviteCode -> groupId
private val wealthTestCounter = java.util.concurrent.atomic.AtomicLong(389L) // 초기값 - 디자인 "389명"

private const val WEALTH_GROUP_MAX_MEMBERS = 50

private fun generateInviteCode(): String {
    val chars = ('a'..'z') + ('0'..'9')
    return (1..8).map { chars.random() }.joinToString("")
}

private fun WealthFortuneResultResponse.amountByAgeGroup(ageGroup: String): Long =
    when (ageGroup) {
        "all" -> graphData.maxOf { it.amount }
        else -> {
            val age = ageGroup.toIntOrNull()
                ?: throw BusinessException(ErrorCode.WEALTH_INVALID_AGE_GROUP)
            graphData.find { it.age == age }?.amount
                ?: throw BusinessException(ErrorCode.WEALTH_INVALID_AGE_GROUP)
        }
    }

fun Route.wealthTestRoutes() {
    // 테스트 실행
    post("/wealth-test") {
        val request = call.receive<WealthTestRequest>()
        request.validate()

        val result = wealthFortuneResults.random()
        val resultId = java.util.UUID.randomUUID().toString()
        wealthResultStore[resultId] = StoredWealthResult(resultId, request.name, result)
        wealthTestCounter.incrementAndGet()

        call.respond(
            HttpStatusCode.OK,
            WealthTestResultResponse(resultId = resultId, name = request.name, result = result)
        )
    }

    // 개인 결과 조회
    get("/wealth-test/results/{resultId}") {
        val resultId = call.requirePathParameter("resultId")
        val stored = wealthResultStore[resultId]
            ?: throw BusinessException(ErrorCode.WEALTH_RESULT_NOT_FOUND)

        call.respond(
            HttpStatusCode.OK,
            WealthTestResultResponse(resultId = stored.resultId, name = stored.name, result = stored.result)
        )
    }

    // 누적 참여자 수
    get("/wealth-test/stats") {
        call.respond(
            HttpStatusCode.OK,
            WealthTestStatsResponse(totalParticipants = wealthTestCounter.get())
        )
    }

    // 그룹 생성
    post("/wealth-test/groups") {
        val request = call.receive<WealthGroupCreateRequest>()
        request.validate()

        request.resultId?.let {
            if (!wealthResultStore.containsKey(it)) {
                throw BusinessException(ErrorCode.WEALTH_RESULT_NOT_FOUND)
            }
        }

        val groupId = java.util.UUID.randomUUID().toString()
        val inviteCode = generateUniqueInviteCode()
        val members = request.resultId?.let { mutableListOf(it) } ?: mutableListOf()

        val group = StoredWealthGroup(
            groupId = groupId,
            groupName = request.groupName,
            inviteCode = inviteCode,
            memberResultIds = members
        )
        wealthGroupStore[groupId] = group
        wealthInviteCodeIndex[inviteCode] = groupId

        call.respond(
            HttpStatusCode.OK,
            WealthGroupCreateResponse(
                groupId = groupId,
                groupName = group.groupName,
                inviteCode = inviteCode,
                memberCount = members.size
            )
        )
    }

    // 초대 코드로 그룹 정보 조회
    get("/wealth-test/groups/invite/{inviteCode}") {
        val inviteCode = call.requirePathParameter("inviteCode")
        val groupId = wealthInviteCodeIndex[inviteCode]
            ?: throw BusinessException(ErrorCode.WEALTH_GROUP_NOT_FOUND)
        val group = wealthGroupStore[groupId]
            ?: throw BusinessException(ErrorCode.WEALTH_GROUP_NOT_FOUND)

        call.respond(
            HttpStatusCode.OK,
            WealthGroupInviteResponse(
                groupId = group.groupId,
                groupName = group.groupName,
                memberCount = group.memberResultIds.size
            )
        )
    }

    // 그룹 가입
    post("/wealth-test/groups/{groupId}/join") {
        val groupId = call.requirePathParameter("groupId")
        val request = call.receive<WealthGroupJoinRequest>()
        request.validate()

        val group = wealthGroupStore[groupId]
            ?: throw BusinessException(ErrorCode.WEALTH_GROUP_NOT_FOUND)

        if (!wealthResultStore.containsKey(request.resultId)) {
            throw BusinessException(ErrorCode.WEALTH_RESULT_NOT_FOUND)
        }

        synchronized(group.memberResultIds) {
            if (group.memberResultIds.size >= WEALTH_GROUP_MAX_MEMBERS) {
                throw BusinessException(ErrorCode.WEALTH_GROUP_FULL)
            }
            if (!group.memberResultIds.contains(request.resultId)) {
                group.memberResultIds.add(request.resultId)
            }
        }

        call.respond(
            HttpStatusCode.OK,
            WealthGroupJoinResponse(
                groupId = group.groupId,
                groupName = group.groupName,
                memberCount = group.memberResultIds.size
            )
        )
    }

    // 그룹 랭킹 조회
    get("/wealth-test/groups/{groupId}/ranking") {
        val groupId = call.requirePathParameter("groupId")
        val ageGroup = call.request.queryParameters["ageGroup"] ?: "all"

        val group = wealthGroupStore[groupId]
            ?: throw BusinessException(ErrorCode.WEALTH_GROUP_NOT_FOUND)

        val members = group.memberResultIds.mapNotNull { wealthResultStore[it] }
        val ranked = members
            .map { stored -> stored to stored.result.amountByAgeGroup(ageGroup) }
            .sortedWith(compareByDescending<Pair<StoredWealthResult, Long>> { it.second }.thenBy { it.first.createdAt })
            .mapIndexed { idx, (stored, amount) ->
                WealthGroupRankingEntry(
                    rank = idx + 1,
                    resultId = stored.resultId,
                    name = stored.name,
                    amount = amount,
                    result = stored.result
                )
            }

        call.respond(
            HttpStatusCode.OK,
            WealthGroupRankingResponse(
                groupName = group.groupName,
                inviteCode = group.inviteCode,
                totalMemberCount = ranked.size,
                ageGroup = ageGroup,
                rankings = ranked
            )
        )
    }
}

private fun generateUniqueInviteCode(): String {
    repeat(10) {
        val code = generateInviteCode()
        if (!wealthInviteCodeIndex.containsKey(code)) return code
    }
    throw InternalServerErrorException()
}

// =============================================================================
// QA Routes
// =============================================================================

fun Route.qaRoutes(userService: UserService) {
    route("/qa/users/{userId}") {
        get("/home-state") {
            val userId = call.requirePathParameter("userId")
            val user = userService.getUserById(userId)

            call.respond(
                HttpStatusCode.OK,
                QaHomeStateResponse(
                    longAbsence = user.qaOverrideLongAbsence,
                    yesterdayMissionSuccess = user.qaOverrideYesterdayMissionSuccess,
                    todayDone = user.qaOverrideTodayDone,
                    isFirstAccess = user.qaOverrideIsFirstAccess
                )
            )
        }

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

        delete("/home-state") {
            val userId = call.requirePathParameter("userId")
            userService.getUserById(userId) // 존재 확인

            userService.updateQaHomeStateOverrides(
                userId,
                longAbsence = null,
                yesterdayMissionSuccess = null,
                todayDone = null,
                isFirstAccess = null
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

/**
 * 미션 정산 공통 메서드
 * @param isExplicit true = /done API 호출, false = 자동 롤오버 (둘 다 배율 적용)
 * @return 적립된 포인트
 */
private suspend fun settleMissions(
    userId: String,
    date: kotlinx.datetime.LocalDate,
    missions: List<Mission>,
    userService: UserService,
    pointMultiplierService: PointMultiplierService,
    isExplicit: Boolean
): Long {
    val basePoint = missions.filter { it.finished }.sumOf { it.calculatePoint() }
    if (basePoint == 0L) return 0L

    val yesterdayHistory = userService.getYesterdayPastRoutines(userId, date)
    val latestHistoryDate = userService.getLatestPastRoutineDate(userId)
    val multiplier = pointMultiplierService.calculateMultiplier(date, yesterdayHistory, latestHistoryDate)

    val earnedPoint = basePoint * multiplier
    userService.addPoint(userId, earnedPoint)
    return earnedPoint
}