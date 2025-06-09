package com.mashup.dhc.utils

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class BirthTime(
    val value: LocalTime?
)