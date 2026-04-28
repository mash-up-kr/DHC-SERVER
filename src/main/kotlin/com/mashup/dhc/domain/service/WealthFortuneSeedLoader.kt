package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.WealthFortune
import com.mashup.dhc.domain.model.WealthFortuneEventDoc
import com.mashup.dhc.domain.model.WealthFortuneGraphPointDoc
import com.mashup.dhc.domain.model.WealthFortuneRepository
import com.mashup.dhc.utils.Image
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.security.MessageDigest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.Document
import org.slf4j.LoggerFactory

/**
 * 금전운(부자 테스트) 콘텐츠 시드 로더.
 *
 * 서버 기동 시 `src/main/resources/seed/wealth_fortune.json` 의 SHA-256 해시를
 * `wealth_fortune_seed_meta` 메타 컬렉션에 저장된 해시와 비교해, 다를 때만
 * `wealth_fortune` 컬렉션을 비우고 재적재한다. 같으면 noop.
 */
class WealthFortuneSeedLoader(
    private val database: MongoDatabase,
    private val wealthFortuneRepository: WealthFortuneRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sync() {
        val resource =
            this::class.java.classLoader.getResourceAsStream(SEED_RESOURCE_PATH)
                ?: run {
                    logger.warn("WealthFortune 시드 리소스가 없음: {}", SEED_RESOURCE_PATH)
                    return
                }
        val rawJson = resource.use { it.readBytes().toString(Charsets.UTF_8) }
        val hash = sha256(rawJson)

        val metaCollection = database.getCollection<Document>(META_COLLECTION)
        val storedMeta = metaCollection.find(Filters.eq("_id", META_ID)).firstOrNull()
        val storedHash = storedMeta?.getString("hash")
        val currentCount = wealthFortuneRepository.count()

        if (storedHash == hash && currentCount > 0) {
            logger.info("WealthFortune 시드 동기화 스킵: hash 동일 ({} 건)", currentCount)
            return
        }

        val dtos = json.decodeFromString<List<WealthFortuneSeedDto>>(rawJson)
        val docs = dtos.map { it.toDomain() }

        val deleted = wealthFortuneRepository.deleteAll()
        val inserted = wealthFortuneRepository.insertMany(docs)
        metaCollection.replaceOne(
            Filters.eq("_id", META_ID),
            Document("_id", META_ID).append("hash", hash).append("count", inserted),
            ReplaceOptions().upsert(true)
        )
        logger.info(
            "WealthFortune 시드 재동기화: deleted={}, inserted={}, hash={}",
            deleted,
            inserted,
            hash.take(12)
        )
    }

    private fun sha256(text: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val SEED_RESOURCE_PATH = "seed/wealth_fortune.json"
        private const val META_COLLECTION = "wealth_fortune_seed_meta"
        private const val META_ID = "version"
    }
}

@Serializable
private data class WealthFortuneSeedDto(
    val fortuneType: String,
    val fortuneTypeDescription: String,
    val fortuneTypeImageUrl: String,
    val fortuneDetail: String,
    val graphData: List<GraphPointDto>,
    val events: List<EventDto>
) {
    fun toDomain(): WealthFortune =
        WealthFortune(
            fortuneType = fortuneType,
            fortuneTypeDescription = fortuneTypeDescription,
            fortuneTypeImageUrl = Image.custom(fortuneTypeImageUrl),
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