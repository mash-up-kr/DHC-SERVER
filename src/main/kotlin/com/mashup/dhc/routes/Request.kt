package com.mashup.dhc.routes

import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserRequest(
    val userToken: String,
    val gender: Gender,
    val birthDate: BirthDate,
    val birthTime: BirthTime?,
    val preferredMissionCategoryList: List<MissionCategory>
) {
    fun validate() {
        val errors = mutableListOf<ErrorCode>()

        if (userToken.isBlank()) {
            errors.add(ErrorCode.EMPTY_USER_TOKEN)
        }

        if (preferredMissionCategoryList.isEmpty()) {
            errors.add(ErrorCode.NO_MISSION_CATEGORY_SELECTED)
        } else if (preferredMissionCategoryList.size > 6) {
            errors.add(ErrorCode.TOO_MANY_MISSION_CATEGORIES)
        }

        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        if (birthDate.date > today) {
            errors.add(ErrorCode.INVALID_BIRTH_DATE)
        }

        birthTime?.let { bt ->
            bt.value?.let { time ->
                if (time.hour !in 0..23 || time.minute !in 0..59) {
                    errors.add(ErrorCode.INVALID_TIME_FORMAT)
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}

@Serializable
data class EndTodayMissionRequest(
    val date: LocalDate
) {
    fun validate() {
        val errors = mutableListOf<ErrorCode>()
        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        if (date > today) {
            errors.add(ErrorCode.FUTURE_MISSION_COMPLETION)
        }

        val daysDifference = today.toEpochDays() - date.toEpochDays()
        if (daysDifference > 30) {
            errors.add(ErrorCode.OLD_MISSION_COMPLETION)
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}

@Serializable
data class ToggleMissionRequest(
    val finished: Boolean? = null,
    val switch: Boolean? = null
) {
    fun validate() {
        val errors = mutableListOf<ErrorCode>()

        if (finished == null && switch == null) {
            errors.add(ErrorCode.INVALID_REQUEST)
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}

@Serializable
data class QaHomeStateRequest(
    val longAbsence: Boolean? = null,
    val yesterdayMissionSuccess: Boolean? = null,
    val todayDone: Boolean? = null,
    val isFirstAccess: Boolean? = null
)

@Serializable
data class QaPointRequest(
    val point: Long
)

@Serializable
data class LoveTestRequest(
    val me: LoveTestMe,
    val you: LoveTestYou,
    val loveDate: LocalDate
) {
    @Serializable
    data class LoveTestMe(
        val gender: Gender,
        val name: String,
        val birthDate: BirthDate,
        val birthTime: BirthTime?
    )

    @Serializable
    data class LoveTestYou(
        val gender: Gender,
        val name: String,
        val birthDate: BirthDate?,
        val birthTime: BirthTime?,
        val additional: String?
    )
}