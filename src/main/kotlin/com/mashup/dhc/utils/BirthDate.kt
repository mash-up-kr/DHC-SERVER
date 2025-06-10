package com.mashup.com.mashup.dhc.utils

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BirthDate(
    val date: LocalDate,
    val calendarType: CalendarType
)

enum class CalendarType {
    SOLAR,
    LUNAR
}