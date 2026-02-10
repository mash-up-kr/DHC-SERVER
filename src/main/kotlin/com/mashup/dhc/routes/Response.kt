package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.ElementBalance
import com.mashup.dhc.domain.model.ElementStatus
import com.mashup.dhc.domain.model.FiveElements
import com.mashup.dhc.domain.model.FortuneCard
import com.mashup.dhc.domain.model.FortuneOverview
import com.mashup.dhc.domain.model.FortuneScoreRange
import com.mashup.dhc.domain.model.FortuneTip
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.YearlyFortune
import com.mashup.dhc.domain.model.toTips
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Image
import com.mashup.dhc.utils.ImageFormat
import com.mashup.dhc.utils.ImageUrlMapper
import com.mashup.dhc.utils.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserResponse(
    val id: String
)

@Serializable
data class SearchUserResponse(
    val id: String
)

@Serializable
data class HomeViewResponse(
    val longTermMission: MissionResponse?,
    val todayDailyMissionList: List<MissionResponse>,
    val todayDailyFortune: FortuneResponse?,
    val todayDone: Boolean,
    val yesterdayMissionSuccess: Boolean,
    val longAbsence: Boolean,
    val isFirstAccess: Boolean,
    val point: Long,
    val testBanner: TestBannerResponse?
)

@Serializable
data class TestBannerResponse(
    val version: Int,
    val title: String,
    val subTitle: String,
    val imageUrl: Image?,
    val testUrl: String?
)

@Serializable
data class RewardProgressViewResponse(
    val user: RewardUserResponse,
    val rewardList: List<RewardItemResponse>
)

@Serializable
data class RewardUserResponse(
    val rewardImageUrl: Image,
    val rewardLevel: RewardLevel,
    val totalPoint: Long,
    val currentLevelPoint: Long,
    val nextLevelRequiredPoint: Long?
) {
    enum class RewardLevel(
        val level: Int,
        val title: String,
        val requiredTotalPoint: Long
    ) {
        LV1(1, "새싹", 0),
        LV2(2, "흙", 100),
        LV3(3, "돌", 250),
        LV4(4, "동", 400),
        LV5(5, "은", 600),
        LV6(6, "금", 800),
        LV7(7, "백금", 1000),
        LV8(8, "다이아", 1300),
        LV9(9, "루비", 1700),
        LV10(10, "프리미엄", 2200);

        companion object {
            fun fromTotalPoint(point: Long): RewardLevel = entries.lastOrNull { point >= it.requiredTotalPoint } ?: LV1

            fun getNextLevel(current: RewardLevel): RewardLevel? = entries.getOrNull(current.ordinal + 1)
        }
    }
}

@Serializable
data class RewardItemResponse(
    val id: Long,
    val title: String,
    val isUnlocked: Boolean,
    val isUsed: Boolean
)

@Serializable
data class MissionResponse(
    val missionId: String,
    val category: String,
    val difficulty: Int,
    val type: MissionType,
    val finished: Boolean,
    val cost: Money,
    val endDate: LocalDate?,
    val title: String,
    val switchCount: Int,
    val dayNumber: Int? = null,
    val remainingDays: Int? = null
) {
    companion object {
        fun from(mission: Mission): MissionResponse =
            MissionResponse(
                missionId = mission.id.toString(),
                category = mission.category.displayName,
                difficulty = mission.difficulty,
                type = mission.type,
                finished = mission.finished,
                cost = mission.cost,
                endDate = mission.endDate,
                title = mission.title,
                switchCount = mission.switchCount
            )

        fun fromLoveMission(
            mission: Mission,
            dayNumber: Int,
            remainingDays: Int
        ): MissionResponse =
            MissionResponse(
                missionId = mission.id.toString(),
                category = mission.category.displayName,
                difficulty = mission.difficulty,
                type = mission.type,
                finished = mission.finished,
                cost = mission.cost,
                endDate = mission.endDate,
                title = mission.title,
                switchCount = mission.switchCount,
                dayNumber = dayNumber,
                remainingDays = remainingDays
            )
    }
}

@Serializable
data class EndTodayMissionResponse(
    val todaySavedMoney: Money,
    val missionSuccess: Boolean,
    val earnedPoint: Long
)

@Serializable
data class ToggleMissionResponse(
    val missions: List<MissionResponse>
)

@Serializable
data class MissionCategoryResponse(
    val name: String,
    val displayName: String,
    val image: Image
) {
    companion object {
        fun from(
            category: MissionCategory,
            format: ImageFormat = ImageFormat.SVG
        ): MissionCategoryResponse =
            MissionCategoryResponse(
                name = category.name,
                displayName = category.displayName,
                image = category.imageUrl(format)
            )
    }
}

@Serializable
data class MyPageResponse(
    val animalCard: AnimalCard,
    val birthDate: BirthDate,
    val birthTime: BirthTime?,
    val preferredMissionCategoryList: List<MissionCategoryResponse>,
    val alarm: Boolean
)

@Serializable
data class AnimalCard(
    val name: String,
    val cardImage: Image?
)

@Serializable
data class AnalysisViewResponse(
    val totalSavedMoney: Money,
    @Deprecated("안씀") val weeklySavedMoney: Money,
    val weeklySpendMoney: Money,
    val monthlySpendMoney: Money,
    val generationMoneyViewResponse: GenerationMoneyViewResponse
)

@Serializable
data class CalendarViewResponse(
    val threeMonthViewResponse: List<AnalysisMonthViewResponse>
)

@Serializable
data class GenerationMoneyViewResponse(
    val generation: String,
    val gender: Gender,
    val averageSpendMoney: Money
)

@Serializable
data class AnalysisMonthViewResponse(
    val month: Int,
    val averageSucceedProbability: Int,
    val calendarDayMissionViews: List<CalendarDayMissionView>
)

@Serializable
data class CalendarDayMissionView(
    val day: Int,
    val date: LocalDate,
    val finishedMissionCount: Int,
    val totalMissionCount: Int
)

@Serializable
data class UploadResponse(
    val url: String
)

@Serializable
data class FortuneResponse(
    val date: String,
    val fortuneTitle: String,
    val fortuneDetail: String,
    val totalScore: Int,
    val positiveScore: Int,
    val negativeScore: Int,
    val tips: List<FortuneTip>,
    val cardInfo: FortuneCard
) {
    companion object {
        fun from(
            dailyFortune: DailyFortune,
            format: ImageFormat = ImageFormat.SVG
        ): FortuneResponse {
            val scoreRange = FortuneScoreRange.fromScore(dailyFortune.totalScore)

            return FortuneResponse(
                date = dailyFortune.date,
                fortuneTitle = dailyFortune.fortuneTitle,
                fortuneDetail = dailyFortune.fortuneDetail,
                positiveScore = dailyFortune.positiveScore,
                negativeScore = dailyFortune.negativeScore,
                totalScore = dailyFortune.totalScore,
                tips = dailyFortune.toTips(format),
                cardInfo =
                    FortuneCard(
                        image =
                            ImageUrlMapper.MainCard.getFortuneCardByScore(
                                dailyFortune.totalScore,
                                ImageFormat.PNG
                            ),
                        title = scoreRange.title,
                        subTitle = scoreRange.subTitle
                    )
            )
        }
    }
}

@Serializable
data class MissionCategoriesResponse(
    val categories: List<MissionCategoryResponse>
)

@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String,
    val details: String? = null
) {
    companion object {
        fun from(
            errorCode: ErrorCode,
            details: String? = null
        ): ErrorResponse =
            ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
                details = details
            )
    }
}

@Serializable
data class ValidationErrorResponse(
    val code: Int,
    val message: String,
    val validationErrors: List<ErrorDetail>
) {
    @Serializable
    data class ErrorDetail(
        val code: Int,
        val message: String
    )

    companion object {
        fun from(errors: List<ErrorCode>): ValidationErrorResponse =
            ValidationErrorResponse(
                code = ErrorCode.VALIDATION_FAILED.code,
                message = ErrorCode.VALIDATION_FAILED.message,
                validationErrors =
                    errors.map {
                        ErrorDetail(code = it.code, message = it.message)
                    }
            )
    }
}

@Serializable
data class AddJulyHistoryResponse(
    val userId: String,
    val year: Int,
    val month: Int,
    val addedDays: Int,
    val totalSavedMoney: Money
)

@Serializable
data class LoveTestViewResponse(
    val score: Int,
    val fortuneDetail: String,
    val fortuneCard: FortuneCard,
    val fortuneTips: List<FortuneTip>,
    // TODO: 위험요소 확인 필요
    val confessDate: LocalDate,
    val confessLocation: String
)

@Serializable
data class CreateShareCodeResponse(
    val shareCode: String
)

@Serializable
data class CompleteShareResponse(
    val shareCode: String,
    val alreadyCompleted: Boolean
)

@Serializable
data class LoveMissionResponse(
    val missionId: String,
    val dayNumber: Int,
    val title: String,
    val finished: Boolean,
    val remainingDays: Int
)

@Serializable
data class CreateYearlyFortuneResponse(
    val success: Boolean
)

@Serializable
data class YearlyFortuneResponse(
    val year: Int,
    val generatedDate: String,
    val totalScore: Int,
    val summaryTitle: String,
    val summaryDetail: String,
    val fortuneOverview: FortuneOverviewResponse,
    val fiveElements: FiveElementsResponse,
    val yearlyEnergyTitle: String,
    val yearlyEnergyDetail: String,
    val tips: YearlyFortuneTipsResponse,
    val cardInfo: FortuneCard
) {
    companion object {
        fun from(yearlyFortune: YearlyFortune): YearlyFortuneResponse {
            val scoreRange = FortuneScoreRange.fromScore(yearlyFortune.totalScore)
            return YearlyFortuneResponse(
                year = yearlyFortune.year,
                generatedDate = yearlyFortune.generatedDate,
                totalScore = yearlyFortune.totalScore,
                summaryTitle = yearlyFortune.summaryTitle,
                summaryDetail = yearlyFortune.summaryDetail,
                fortuneOverview = FortuneOverviewResponse.from(yearlyFortune.fortuneOverview),
                fiveElements = FiveElementsResponse.from(yearlyFortune.fiveElements),
                yearlyEnergyTitle = yearlyFortune.yearlyEnergyTitle,
                yearlyEnergyDetail = yearlyFortune.yearlyEnergyDetail,
                tips =
                    YearlyFortuneTipsResponse(
                        luckyMenu = yearlyFortune.luckyMenu,
                        luckyColor = yearlyFortune.luckyColor,
                        luckyColorHex = yearlyFortune.luckyColorHex,
                        unluckyMenu = yearlyFortune.unluckyMenu,
                        unluckyColor = yearlyFortune.unluckyColor,
                        unluckyColorHex = yearlyFortune.unluckyColorHex
                    ),
                cardInfo =
                    FortuneCard(
                        image = ImageUrlMapper.MainCard.getFortuneCardByScore(yearlyFortune.totalScore, ImageFormat.PNG),
                        title = scoreRange.title,
                        subTitle = scoreRange.subTitle
                    )
            )
        }
    }
}

@Serializable
data class FortuneOverviewResponse(
    val money: FortuneCategoryResponse,
    val love: FortuneCategoryResponse,
    val study: FortuneCategoryResponse
) {
    companion object {
        fun from(overview: FortuneOverview): FortuneOverviewResponse =
            FortuneOverviewResponse(
                money = FortuneCategoryResponse(overview.money.title, overview.money.description),
                love = FortuneCategoryResponse(overview.love.title, overview.love.description),
                study = FortuneCategoryResponse(overview.study.title, overview.study.description)
            )
    }
}

@Serializable
data class FortuneCategoryResponse(
    val title: String,
    val description: String
)

@Serializable
data class FiveElementsResponse(
    val dominantElement: String,
    val dominantWarning: String,
    val wood: ElementBalanceResponse,
    val fire: ElementBalanceResponse,
    val earth: ElementBalanceResponse,
    val metal: ElementBalanceResponse,
    val water: ElementBalanceResponse
) {
    companion object {
        fun from(elements: FiveElements): FiveElementsResponse =
            FiveElementsResponse(
                dominantElement = elements.dominantElement,
                dominantWarning = elements.dominantWarning,
                wood = ElementBalanceResponse.from(elements.wood),
                fire = ElementBalanceResponse.from(elements.fire),
                earth = ElementBalanceResponse.from(elements.earth),
                metal = ElementBalanceResponse.from(elements.metal),
                water = ElementBalanceResponse.from(elements.water)
            )
    }
}

@Serializable
data class ElementBalanceResponse(
    val percentage: Int,
    val status: String
) {
    companion object {
        fun from(balance: ElementBalance): ElementBalanceResponse =
            ElementBalanceResponse(
                percentage = balance.percentage,
                status =
                    when (balance.status) {
                        ElementStatus.BALANCED -> "적정"
                        ElementStatus.EXCESS -> "과다"
                        ElementStatus.DEFICIENT -> "부족"
                    }
            )
    }
}

@Serializable
data class YearlyFortuneTipsResponse(
    val luckyMenu: String,
    val luckyColor: String,
    val luckyColorHex: String,
    val unluckyMenu: String,
    val unluckyColor: String,
    val unluckyColorHex: String
)