package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.WealthFortune
import com.mashup.dhc.domain.model.WealthFortuneEventDoc
import com.mashup.dhc.domain.model.WealthFortuneGraphPointDoc
import com.mashup.dhc.domain.model.WealthFortuneRepository
import com.mashup.dhc.utils.Image
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * 금전운(부자 테스트) 콘텐츠 시드 로더.
 *
 * 서버 기동 시 `wealth_fortune` 컬렉션이 비어 있으면
 * `src/main/resources/seed/wealth_fortune.json` 의 300개 엔트리를 일괄 적재한다.
 * 한 번 적재되면 다시 실행해도 noop.
 */
class WealthFortuneSeedLoader(
    private val wealthFortuneRepository: WealthFortuneRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty() {
        val current = wealthFortuneRepository.count()
        if (current > 0) {
            logger.info("WealthFortune 시드 스킵: 이미 {} 건 적재되어 있음", current)
            return
        }

        val resource =
            this::class.java.classLoader.getResourceAsStream(SEED_RESOURCE_PATH)
                ?: run {
                    logger.warn("WealthFortune 시드 리소스가 없음: {}", SEED_RESOURCE_PATH)
                    return
                }

        val rawJson = resource.use { it.readBytes().toString(Charsets.UTF_8) }
        val dtos = json.decodeFromString<List<WealthFortuneSeedDto>>(rawJson)
        val docs = dtos.map { it.toDomain() }

        val inserted = wealthFortuneRepository.insertMany(docs)
        logger.info("WealthFortune 시드 적재 완료: {} 건 (요청 {}건)", inserted, docs.size)
    }

    companion object {
        private const val SEED_RESOURCE_PATH = "seed/wealth_fortune.json"
    }
}

@Serializable
private data class WealthFortuneSeedDto(
    val fortuneType: String,
    val fortuneTypeDescription: String,
    val fortuneDetail: String,
    val graphData: List<GraphPointDto>,
    val events: List<EventDto>
) {
    fun toDomain(): WealthFortune =
        WealthFortune(
            fortuneType = fortuneType,
            fortuneTypeDescription = fortuneTypeDescription,
            fortuneDetail = fortuneDetail,
            graphData = graphData.map { WealthFortuneGraphPointDoc(it.age, it.amount) },
            events =
                events.map {
                    WealthFortuneEventDoc(
                        age = it.age,
                        description = it.description,
                        amount = it.amount,
                        iconUrl = Image.custom(it.iconUrl)
                    )
                }
        )
}

@Serializable
private data class GraphPointDto(
    val age: Int,
    val amount: Long
)

@Serializable
private data class EventDto(
    val age: Int,
    val description: String,
    val amount: Long,
    val iconUrl: String
)