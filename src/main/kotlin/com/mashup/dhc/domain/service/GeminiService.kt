package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class GeminiService(
    private val apiKey: String,
    private val systemInstruction: String
) {
    private val log = LoggerFactory.getLogger(GeminiService::class.java)

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    }

    /**
     * JSON 스키마를 lazy 초기화로 한 번만 로드하고 캐시
     * 처음 접근할 때만 파일을 읽고, 이후에는 캐시된 값을 재사용
     */
    private val responseSchema: JsonElement by lazy { loadResponseSchema() }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        // 타임아웃 설정 추가
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 2분
            connectTimeoutMillis = 30_000   // 30초
            socketTimeoutMillis = 120_000   // 2분
        }
    }

    suspend fun generateFortune(request: GeminiFortuneRequest): GeminiFortuneResponse {
        val startTime = System.currentTimeMillis()

        return try {
            log.info("Gemini API 호출 시작 - 사용자: ${request.gender}, 생년월일: ${request.birthDate}, 월: ${request.month}")

            val geminiRequest = request.toPrompt().toGeminiRequest()
            val response = client.post(BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
                timeout { requestTimeoutMillis = 120_000 }
            }

            val geminiResponse: GeminiApiResponse = response.body()
            val responseTime = System.currentTimeMillis() - startTime

            val responseText = geminiResponse.validateAndExtractText(responseTime)
            val result = Json.decodeFromString<GeminiFortuneResponse>(responseText)

            log.info("Gemini API 호출 성공 - 응답 시간: {}ms, 운세 데이터 수: {}개", responseTime, result.fortune.size)
            result

        } catch (e: Exception) {
            handleApiError(e, System.currentTimeMillis() - startTime)
        }
    }

    /**
     * Gemini API 응답 검증 및 텍스트 추출
     */
    private fun GeminiApiResponse.validateAndExtractText(responseTime: Long): String {
        // API 오류 체크
        error?.let { error ->
            log.error("Gemini API 오류 - 코드: {}, 메시지: {}, 응답시간: {}ms", error.code, error.message, responseTime)
            throw Exception("Gemini API 오류: ${error.message ?: "알 수 없는 오류"} (코드: ${error.code})")
        }

        // 응답 체인 검증
        return candidates
            ?.firstOrNull()?.content
            ?.parts?.firstOrNull()?.text
            ?.takeIf { it.isNotBlank() }
            ?: run {
                log.error("Gemini API 응답 구조 오류 - 응답시간: {}ms", responseTime)
                throw Exception("Gemini API 응답에 유효한 텍스트가 없습니다.")
            }
    }

    /**
     * API 오류 처리 및 로깅
     */
    private fun handleApiError(e: Exception, responseTime: Long): Nothing {
        when (e) {
            is HttpRequestTimeoutException -> log.error("Gemini API 타임아웃 - 응답시간: {}ms", responseTime)
            is ClientRequestException -> log.error("Gemini API 클라이언트 오류 - HTTP {}, 응답시간: {}ms", e.response.status, responseTime)
            is ServerResponseException -> log.error("Gemini API 서버 오류 - HTTP {}, 응답시간: {}ms", e.response.status, responseTime)
            else -> {
                if (e.message?.contains("Gemini API") != true)
                    log.error("예상치 못한 오류 - {}: {}, 응답시간: {}ms", e::class.simpleName, e.message, responseTime)
            }
        }
        throw e
    }

    private fun GeminiFortuneRequest.toPrompt(): String {
        return """
            $systemInstruction
            
            사용자 정보:
            - 성별: ${gender}
            - 생년월일: ${birthDate}
            - 출생시간: ${birthTime}
            - 요청 년도: ${year}년
            - 요청 월: ${month}월
            
            위 정보를 바탕으로 ${year}년 ${month}월 한 달간의 금전운을 일별로 분석해주세요.
            응답은 반드시 지정된 JSON 스키마 형식으로 제공해주세요.
        """.trimIndent()
    }

    private fun String.toGeminiRequest(): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(this))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = responseSchema // 캐시된 스키마 사용
            )
        )
    }

    /**
     * JSON 스키마 로드 - 한 번만 실행되고 이후에는 캐시된 값 사용
     * lazy 초기화로 스레드 세이프하게 처리
     * 로드 실패시 애플리케이션 종료
     */
    private fun loadResponseSchema(): JsonElement {
        log.info("JSON 스키마 로드 시작")

        val schemaResource = this::class.java.classLoader
            .getResourceAsStream("gemini-response-schema.json")
            ?: throw IllegalStateException("필수 리소스 파일 'gemini-response-schema.json'을 찾을 수 없습니다. 애플리케이션을 종료합니다.")

        val schemaText = schemaResource.bufferedReader().use { it.readText() }
        val schema = Json.parseToJsonElement(schemaText)

        log.info("JSON 스키마 로드 완료 - 외부 파일에서 성공적으로 로드됨")
        return schema
    }
}

// Gemini API 요청 스키마
@Serializable
data class GeminiFortuneRequest(
    val gender: String, // "male" | "female"
    @SerialName("birth_date") val birthDate: String, // "YYYY-MM-DD"
    @SerialName("birth_time") val birthTime: String?, // "HH:MM"
    val year: Int,
    val month: Int
)

// Gemini API 응답 스키마
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