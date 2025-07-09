package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneColor
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Image
import com.mashup.dhc.utils.ImageFormat
import com.mashup.dhc.utils.ImageUrlMapper
import com.mashup.dhc.utils.ImageUrlMapper.MainCard.getFourLeafClover
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
    val weeklySavedMoney: Money,
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
    val jinxedColor: String,
    val jinxedColorHex: String,
    val jinxedColorImage: Image,
    val jinxedMenu: String,
    val jinxedMenuImage: Image,
    val jinxedNumber: Int,
    val luckyColor: String,
    val luckyColorHex: String,
    val luckyColorImage: Image,
    val luckyNumber: Int,
    val positiveScore: Int,
    val negativeScore: Int,
    val todayMenu: String,
    val todayMenuImage: Image,
    val totalScore: Int,
    val luckyColorType: FortuneColor,
    val jinxedColorType: FortuneColor,
    val fortuneCardImage: Image,
) {
    companion object {
        fun from(
            dailyFortune: DailyFortune,
            format: ImageFormat = ImageFormat.SVG
        ): FortuneResponse =
            FortuneResponse(
                date = dailyFortune.date,
                fortuneTitle = dailyFortune.fortuneTitle,
                fortuneDetail = dailyFortune.fortuneDetail,
                jinxedColor = dailyFortune.jinxedColor,
                jinxedColorHex = dailyFortune.jinxedColorHex,
                jinxedColorImage = ImageUrlMapper.Fortune.getJinxedColorImageUrl(format),
                jinxedMenu = dailyFortune.jinxedMenu,
                jinxedMenuImage = ImageUrlMapper.Fortune.getJinxedMenuImageUrl(format),
                jinxedNumber = dailyFortune.jinxedNumber,
                jinxedColorType = dailyFortune.jinxedColorType,
                positiveScore = dailyFortune.positiveScore,
                negativeScore = dailyFortune.negativeScore,
                todayMenu = dailyFortune.todayMenu,
                todayMenuImage = ImageUrlMapper.Fortune.getTodayMenuImageUrl(format),
                totalScore = dailyFortune.totalScore,
                luckyColor = dailyFortune.luckyColor,
                luckyColorHex = dailyFortune.luckyColorHex,
                luckyColorImage = ImageUrlMapper.Fortune.getLuckyColorImageUrl(format),
                luckyColorType = dailyFortune.luckyColorType,
                luckyNumber = dailyFortune.luckyNumber,
                fortuneCardImage = getFourLeafClover() // TODO: 디자인 작업 완료 후 교체
            )
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