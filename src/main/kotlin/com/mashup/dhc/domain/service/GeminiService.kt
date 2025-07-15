package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.utils.batch.GeminiBatchProcessor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory

class GeminiService(
    private val apiKey: String,
    private val systemInstruction: String,
    private val fortuneRepository: FortuneRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val monthResponseSchema: JsonElement by lazy { loadMonthResponseSchema() }
    private val dailyBatchResponseSchema: JsonElement by lazy { loadDailyBatchResponseSchema() }
    private val batchProcessor = GeminiBatchProcessor(this)

    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 600_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 600_000
            }
        }

    // 배치 프로세서 시작 (서버 시작 시 호출)
    fun startBatchProcessor(scope: CoroutineScope) {
        logger.info("Gemini 요청 배치 프로세스 시작")
        batchProcessor.startBatchProcessor(scope)
    }

    suspend fun generateMonthlyFortuneWithBatch(
        userId: String,
        request: GeminiFortuneRequest
    ): GeminiFortuneResponse =
        batchProcessor.generateMonthlyFortune(userId, request) { monthlyFortune ->
            fortuneRepository.upsertMonthlyFortune(userId, monthlyFortune)
        }

    suspend fun generateDailyFortuneBatch(requests: List<Pair<String, GeminiFortuneRequest>>) {
        batchProcessor.generateDailyFortune(requests) { map ->
            suspend {
                for ((userId, dailyFortunes) in map.entries) {
                    fortuneRepository.upsertDailyFortunes(userId, dailyFortunes)
                }
            }
        }
    }

    suspend fun requestMonthFortune(request: GeminiFortuneRequest): GeminiFortuneResponse {
        val geminiRequest = request.toMonthPrompt().toGeminiMonthRequest()
        val response =
            client.post(BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
                timeout { requestTimeoutMillis = 600_000 }
            }

        val geminiResponse: GeminiApiResponse = response.body()
        val responseText = geminiResponse.validateAndExtractText()
        val geminiFortuneResponse = Json.decodeFromString<GeminiFortuneResponse>(responseText)

        val monthlyFortune = geminiFortuneResponse.toMonthlyFortune()
        try {
            monthlyFortune.dailyFortuneList.forEach { fortuneRepository.insertDailyOne(it) }
        } catch (ex: Exception) {
            logger.info("daily fortune update failed.")
        }
        return geminiFortuneResponse
    }

    suspend fun requestDailyBatchFortune(
        requests: List<Pair<String, GeminiFortuneRequest>>
    ): GeminiBatchFortuneResponse {
        val batchPrompt = createDailyBatchPrompt(requests.map { it.first to it.second })
        val geminiRequest = batchPrompt.toGeminiMultiUserDailyBatchRequest()

        val response =
            client.post(BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
                timeout { requestTimeoutMillis = 100_000 }
            }

        val geminiResponse: GeminiApiResponse = response.body()
        val responseText = geminiResponse.validateAndExtractText()
        return Json.decodeFromString<GeminiBatchFortuneResponse>(responseText)
    }

    private fun createDailyBatchPrompt(requests: List<Pair<String, GeminiFortuneRequest>>): String {
        val userInfoList =
            requests
                .mapIndexed { index, (userId, request) ->
                    """
                    사용자 ${index + 1} (ID: $userId):
                    - 성별: ${request.gender}
                    - 생년월일: ${request.birthDate}
                    - 출생시간: ${request.birthTime}
                    - 요청 년도: ${request.year}년
                    - 요청 월: ${request.month}월
                    """.trimIndent()
                }.joinToString("\n\n")

        return """
            $systemInstruction
            
            여러 사용자의 금전운을 동시에 분석해주세요:
            
            $userInfoList
            
            각 사용자별로 해당 년월의 하루 금전운을 일별로 분석해주세요.
            응답은 반드시 지정된 JSON 스키마 형식으로 제공하며, results 배열에 각 사용자의 결과를 user_id와 함께 포함해주세요.
            """.trimIndent()
    }

    private fun GeminiFortuneRequest.toMonthPrompt(): String =
        """
        $systemInstruction
        
        사용자 정보:
        - 성별: $gender
        - 생년월일: $birthDate
        - 출생시간: $birthTime
        - 요청 년도: ${year}년
        - 요청 월: ${month}월
        
        위 정보를 바탕으로 ${year}년 ${month}월 한 달간의 금전운을 일별로 분석해주세요.
        응답은 반드시 지정된 JSON 스키마 형식으로 제공해주세요.
        """.trimIndent()

    private fun String.toGeminiMonthRequest(): GeminiRequest =
        GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(this)))),
            generationConfig =
                GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = monthResponseSchema
                )
        )

    private fun String.toGeminiMultiUserDailyBatchRequest(): GeminiRequest =
        GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(this)))),
            generationConfig =
                GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = dailyBatchResponseSchema
                )
        )

    private fun GeminiApiResponse.validateAndExtractText(): String {
        error?.let { error ->
            throw Exception("Gemini API 오류: ${error.message ?: "알 수 없는 오류"} (코드: ${error.code})")
        }

        return candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: throw Exception("Gemini API 응답에 유효한 텍스트가 없습니다.")
    }

    private fun loadMonthResponseSchema(): JsonElement {
        val schemaResource =
            this::class.java.classLoader
                .getResourceAsStream("gemini-response-schema.json")
                ?: throw IllegalStateException("gemini-response-schema.json을 찾을 수 없습니다.")

        return Json.parseToJsonElement(schemaResource.bufferedReader().use { it.readText() })
    }

    private fun loadDailyBatchResponseSchema(): JsonElement {
        val schemaResource =
            this::class.java.classLoader
                .getResourceAsStream("gemini-batch-response-schema.json")
                ?: throw IllegalStateException("gemini-batch-response-schema.json을 찾을 수 없습니다.")

        return Json.parseToJsonElement(schemaResource.bufferedReader().use { it.readText() })
    }

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    }
}

@Serializable
data class GeminiBatchFortuneResponse(
    val results: List<BatchUserResult>
)

@Serializable
data class BatchUserResult(
    @SerialName("user_id") val userId: String,
    val month: Int,
    val year: Int,
    val fortune: List<DailyFortune>
)

@Serializable
data class GeminiFortuneRequest(
    val gender: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("birth_time") val birthTime: String?,
    val year: Int,
    val month: Int
)

@Serializable
data class GeminiFortuneResponse(
    val month: Int,
    val year: Int,
    val fortune: List<DailyFortune>
) {
    fun toMonthlyFortune(): MonthlyFortune =
        MonthlyFortune(
            year = year,
            month = month,
            dailyFortuneList = fortune
        )
}

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

fun User.toGeminiFortuneRequest(): GeminiFortuneRequest =
    now().let {
        GeminiFortuneRequest(
            this.gender.name,
            this.birthDate.toString(),
            this.birthTime?.toString(),
            it.year,
            it.monthNumber
        )
    }