package com.mashup.dhc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

// 요청 스키마
@Serializable
data class FortuneRequest(
    val gender: String, // "male" | "female"
    @SerialName("birth_date") val birthDate: String, // "YYYY-MM-DD"
    @SerialName("birth_time") val birthTime: String?, // "HH:MM"
    val year: Int,
    val month: Int
)

// Gemini API 응답 스키마 (header, body 제외)
@Serializable
data class GeminiFortuneResponse(
    val month: Int,
    val year: Int,
    val fortune: List<DailyFortune>
) {
    fun toMonthlyFortune(): MonthlyFortune {
        return MonthlyFortune(
            year = year,
            month = month,
            dailyFortuneList = fortune
        )
    }
}

// Gemini API에서 받는 일일 운세 객체
@Serializable
data class DailyFortune(
    val date: String,
    @SerialName("fortune_title") val fortuneTitle: String,
    @SerialName("fortune_detail") val fortuneDetail: String,
    @SerialName("jinxed_color") val jinxedColor: String,
    @SerialName("jinxed_color_hex") val jinxedColorHex: String,
    @SerialName("jinxed_menu") val jinxedMenu: String,
    @SerialName("jinxed_number") val jinxedNumber: Int,
    @SerialName("lucky_color") val luckyColor: String,
    @SerialName("lucky_color_hex") val luckyColorHex: String,
    @SerialName("lucky_number") val luckyNumber: Int,
    val score: Int,
    @SerialName("today_menu") val todayMenu: String
)

// MongoDB 캐시 모델
data class MonthlyFortune(
    @BsonId val id: String = ObjectId().toString(),
    val month: Int,
    val year: Int,
    val dailyFortuneList: List<DailyFortune>, // Gemini 응답 저장
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30일 후 만료
)

// Gemini API 요청 구조
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    val responseSchema: JsonElement
)

// Gemini API 응답 구조 (오류 처리를 위해 옵셔널로 변경)
@Serializable
data class GeminiApiResponse(
    val candidates: List<Candidate>? = null,
    val error: ApiError? = null
)

@Serializable
data class Candidate(
    val content: ContentResponse? = null,
    val finishReason: String? = null
)

@Serializable
data class ContentResponse(
    val parts: List<PartResponse>? = null,
    val role: String? = null
)

@Serializable
data class PartResponse(
    val text: String? = null
)

@Serializable
data class ApiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
) 