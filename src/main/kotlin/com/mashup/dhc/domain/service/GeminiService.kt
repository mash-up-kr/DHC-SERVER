package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.LoveTestGeminiResponse
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.YearlyFortune
import com.mashup.dhc.routes.LoveTestRequest
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
    private val yearlyFortuneResponseSchema: JsonElement by lazy { loadYearlyFortuneResponseSchema() }
    private val loveTestResponseSchema: JsonElement by lazy { loadLoveTestResponseSchema() }
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
            for ((userId, dailyFortunes) in map.entries) {
                fortuneRepository.upsertDailyFortunes(userId, dailyFortunes)
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
        logTokenUsage("MonthFortune", geminiResponse.usageMetadata)
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
        logTokenUsage("DailyBatchFortune", geminiResponse.usageMetadata)
        val responseText = geminiResponse.validateAndExtractText()
        return Json.decodeFromString<GeminiBatchFortuneResponse>(responseText)
    }

    suspend fun requestYearlyFortune(request: GeminiFortuneRequest): YearlyFortune {
        val prompt = request.toYearlyFortunePrompt()
        val geminiRequest = prompt.toGeminiYearlyFortuneRequest()

        val response =
            client.post(BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
                timeout { requestTimeoutMillis = 600_000 }
            }

        val geminiResponse: GeminiApiResponse = response.body()
        logTokenUsage("YearlyFortune", geminiResponse.usageMetadata)
        val responseText = geminiResponse.validateAndExtractText()

        val yearlyFortuneResponse = Json.decodeFromString<YearlyFortune>(responseText)
        return yearlyFortuneResponse.copy(generatedDate = now().toString())
    }

    suspend fun requestLoveTest(request: LoveTestRequest): LoveTestGeminiResponse {
        val prompt = request.toLoveTestPrompt()
        val geminiRequest = prompt.toGeminiLoveTestRequest()

        val response =
            client.post(BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(geminiRequest)
                timeout { requestTimeoutMillis = 600_000 }
            }

        val geminiResponse: GeminiApiResponse = response.body()
        logTokenUsage("LoveTest", geminiResponse.usageMetadata)
        val responseText = geminiResponse.validateAndExtractText()
        return Json.decodeFromString<LoveTestGeminiResponse>(responseText)
    }

    private fun logTokenUsage(
        requestType: String,
        usage: UsageMetadata?
    ) {
        if (usage != null) {
            logger.info(
                "[Gemini Token Usage] type={}, promptTokens={}, responseTokens={}, totalTokens={}",
                requestType,
                usage.promptTokenCount ?: 0,
                usage.candidatesTokenCount ?: 0,
                usage.totalTokenCount ?: 0
            )
        } else {
            logger.warn("[Gemini Token Usage] type={}, usageMetadata is null", requestType)
        }
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
            
            각 사용자별로 요청 년도, 요청 월에서 ${now().dayOfMonth}일 부터 5일치 하루 금전운을 일별로 분석해주세요.
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

    private fun GeminiFortuneRequest.toYearlyFortunePrompt(): String =
        """
        $systemInstruction

        사용자 정보:
        - 성별: $gender
        - 생년월일(양력): $birthDate
        - 출생시간: ${birthTime ?: "모름"}
        - 분석 대상 년도(세운): ${year}년

        아래 절차를 엄격히 따라 ${year}년 1년 운세를 분석해주세요.
        같은 생년월일시 입력에 대해서는 항상 같은 사주 원국이 나와야 하며, 호출마다 결과가 달라지면 안 됩니다.

        [1단계] 사주 원국(四柱 原局) 세우기 — 내부 계산, 출력하지 말 것
        1. 입력 생년월일시를 양력 기준으로 해석하고, 만세력에 따라 연주(年柱)·월주(月柱)·일주(日柱)·시주(時柱)를 세운다.
           출생시간이 "모름"이면 시주를 제외하고 연·월·일주 3주(6글자)만 사용한다.
        2. 사주 각 글자의 오행을 카운트한다. 기준은 아래를 엄격히 따른다.
           - 천간 오행: 갑·을 = 목, 병·정 = 화, 무·기 = 토, 경·신 = 금, 임·계 = 수
           - 지지 본기 오행: 인·묘 = 목, 사·오 = 화, 진·술·축·미 = 토, 신·유 = 금, 해·자 = 수
        3. 일간(日柱의 천간)을 해석의 기준점으로 삼는다.

        [2단계] 오행 비율/상태 산출 — 이 결과가 five_elements 필드가 된다
        - five_elements.{wood|fire|earth|metal|water}.percentage
          → **사주 원국 기준** 오행 비율(%). 5개의 합은 100(±2 허용).
          → 시주가 없으면 6글자 기준, 있으면 8글자 기준으로 단순 카운트 비율을 쓴다.
        - five_elements.{...}.status
          → 원국 비율에 ${year}년 세운(그 해 연주 오행)을 합산해 최종 판단.
          → 합산 비율이 30% 이상이면 EXCESS, 10% 이하이면 DEFICIENT, 그 외 BALANCED.
          → 세운이 원국의 같은 오행을 더하면 EXCESS 쪽으로, 극(剋)하면 DEFICIENT 쪽으로 조정.
        - five_elements.dominant_element
          → 원국 + 세운 합산 후 **가장 비율이 높은** 오행을 "목기운/화기운/토기운/금기운/수기운" 중 하나로.
        - five_elements.dominant_warning
          → 해당 오행이 과다할 때의 경고 1문장.

        ⚠️ 주의: 원국 계산 없이 "요청 연도가 2026년이니까 화가 강할 것 같다" 식으로 추측하지 말 것.
        반드시 생년월일시의 원국 8글자(또는 6글자)를 먼저 세우고, 그 **실제 오행 분포**를 percentage에 반영할 것.
        일간이 목이라고 해서 목이 많은 게 아니다 — 일간은 해석의 중심일 뿐, 원국 내 글자 수를 세야 한다.

        [3단계] 나머지 필드 생성
        1. total_score: ${year}년 전체 운세 점수 (0-100) — 원국과 세운의 상생/상극 관계를 반영
        2. summary_title / summary_detail: 1년 운세 요약 제목과 상세
        3. fortune_overview: 금전운/연애운/학업운 각각의 title과 description
        4. yearly_energy_title / yearly_energy_detail: ${year}년 주요 기운 변화 (dominant_element와 일관되게)
        5. lucky_menu / lucky_color(+hex) / unlucky_menu / unlucky_color(+hex):
           → 부족한 오행을 보충하고 과다한 오행을 덜어주는 방향으로 선택
           → lucky/unlucky 색상은 서로 달라야 함

        응답은 반드시 지정된 JSON 스키마 형식으로만 제공해주세요.
        """.trimIndent()

    private fun String.toGeminiYearlyFortuneRequest(): GeminiRequest =
        GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(this)))),
            generationConfig =
                GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = yearlyFortuneResponseSchema
                )
        )

    private fun LoveTestRequest.toLoveTestPrompt(): String {
        val meInfo =
            """
            - 성별: ${me.gender}
            - 이름: ${me.name}
            - 생년월일: ${me.birthDate}
            - 출생시간: ${me.birthTime ?: "모름"}
            """.trimIndent()

        val youInfo = buildString {
            appendLine("- 성별: ${you.gender}")
            appendLine("- 이름: ${you.name}")
            appendLine("- 생년월일: ${you.birthDate ?: "모름"}")
            appendLine("- 출생시간: ${you.birthTime ?: "모름"}")
            you.additional?.let { appendLine("- 추가정보: $it") }
        }.trimEnd()

        return """
            $systemInstruction

            두 사람의 궁합을 사주팔자 기반으로 분석해주세요:

            [나]
            $meInfo

            [상대방]
            $youInfo

            연애 시작일: $loveDate

            다음 항목들을 분석해주세요:
            1. 궁합 점수 (0-100)
            2. 궁합 상세 설명 (두 사람의 사주 궁합을 재미있고 자세하게 분석)
            3. 추천 데이트 메뉴 / 피해야 할 음식
            4. 행운의 색상(이름+헥스코드) / 피해야 할 색상(이름+헥스코드)
            5. 추천 고백 날짜 (YYYY-MM-DD 형식) / 추천 고백 장소

            응답은 반드시 지정된 JSON 스키마 형식으로 제공해주세요.
            """.trimIndent()
    }

    private fun String.toGeminiLoveTestRequest(): GeminiRequest =
        GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(this)))),
            generationConfig =
                GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = loveTestResponseSchema
                )
        )

    private fun GeminiApiResponse.validateAndExtractText(): String {
        error?.let { error ->
            if (error.code == 503) {
                throw com.mashup.dhc.routes.ExternalServiceUnavailableException(
                    "Gemini API 일시적 과부하 (503)"
                )
            }
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

    private fun loadYearlyFortuneResponseSchema(): JsonElement {
        val schemaResource =
            this::class.java.classLoader
                .getResourceAsStream("gemini-yearly-fortune-schema.json")
                ?: throw IllegalStateException("gemini-yearly-fortune-schema.json을 찾을 수 없습니다.")

        return Json.parseToJsonElement(schemaResource.bufferedReader().use { it.readText() })
    }

    private fun loadLoveTestResponseSchema(): JsonElement {
        val schemaResource =
            this::class.java.classLoader
                .getResourceAsStream("gemini-love-test-schema.json")
                ?: throw IllegalStateException("gemini-love-test-schema.json을 찾을 수 없습니다.")

        return Json.parseToJsonElement(schemaResource.bufferedReader().use { it.readText() })
    }

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
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
    val error: ApiError? = null,
    val usageMetadata: UsageMetadata? = null
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
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