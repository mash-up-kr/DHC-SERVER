package com.mashup.dhc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.bson.types.ObjectId
import java.time.DayOfWeek

// 요청 스키마
@Serializable
data class FortuneRequest(
    val sex: String, // "male" | "female"
    @SerialName("birth_date") val birthDate: String, // "YYYY-MM-DD"
    @SerialName("birth_time") val birthTime: String, // "HH:MM"
    val year: String, // "YYYY"
    val month: String // "MM"
)

// Gemini API 응답 스키마 (header, body 제외)
@Serializable
data class GeminiFortuneResponse(
    val month: String,
    val fortune: List<GeminiDailyFortune>,
    val year: String
)

// Gemini API에서 받는 일일 운세 (header, body 제외)
@Serializable
data class GeminiDailyFortune(
    val date: String,
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

// 클라이언트 응답용 스키마 (기존과 동일)
@Serializable
data class FortuneResponse(
    val month: String,
    val fortune: List<DailyFortune>,
    val year: String
)

// 클라이언트 응답용 일일 운세 (header, body 포함)
@Serializable
data class DailyFortune(
    val header: String,
    val body: String,
    val date: String,
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
@Serializable
data class FortuneCache(
    @SerialName("_id") val id: String = ObjectId().toString(),
    val userToken: String,
    val month: String,
    val year: String,
    val sex: String,
    val birthDate: String,
    val birthTime: String,
    val response: GeminiFortuneResponse, // Gemini 응답 저장
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30일 후 만료
)

// 금전운 점수 구간
enum class FortuneScoreRange(val range: IntRange, val level: String) {
    VERY_BAD(0..20, "매우 나쁨"),
    BAD(21..40, "나쁨"),
    NORMAL(41..60, "보통"),
    GOOD(61..80, "좋음"),
    VERY_GOOD(81..100, "매우 좋음")
}

// 요일별 특성
enum class DayOfWeekCharacter(val dayOfWeek: DayOfWeek, val character: String, val description: String) {
    MONDAY(DayOfWeek.MONDAY, "신중함", "계획적인 소비가 중요한"),
    TUESDAY(DayOfWeek.TUESDAY, "도전적", "새로운 투자 기회를 모색하는"),
    WEDNESDAY(DayOfWeek.WEDNESDAY, "균형감", "수입과 지출의 균형을 맞추는"),
    THURSDAY(DayOfWeek.THURSDAY, "성장", "자산 증대에 집중하는"),
    FRIDAY(DayOfWeek.FRIDAY, "활동적", "적극적인 금전 관리가 필요한"),
    SATURDAY(DayOfWeek.SATURDAY, "여유로움", "합리적인 소비를 하는"),
    SUNDAY(DayOfWeek.SUNDAY, "안정", "안전한 자산 관리에 집중하는")
}

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