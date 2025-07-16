package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneCard
import com.mashup.dhc.domain.model.FortuneScoreRange
import com.mashup.dhc.domain.model.FortuneTip
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
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
    val todayDone: Boolean
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
    val switchCount: Int
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
    }
}

@Serializable
data class EndTodayMissionResponse(
    val todaySavedMoney: Money
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