package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Image
import com.mongodb.MongoException
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

// =============================================================================
// 부자 테스트 도메인 모델 (MongoDB)
// =============================================================================

/**
 * 금전운 콘텐츠 시드. 500개를 미리 적재하고 `$sample`로 1개를 무작위 선택한다.
 * graphData/events 는 결과 응답에 그대로 사용되는 구조와 동일하게 보관.
 */
data class WealthFortune(
    @BsonId val id: ObjectId? = null,
    val fortuneType: String,
    val fortuneTypeDescription: String,
    val fortuneDetail: String,
    val graphData: List<WealthFortuneGraphPointDoc>,
    val events: List<WealthFortuneEventDoc>
)

data class WealthFortuneGraphPointDoc(
    val age: Int,
    val amount: Long
)

data class WealthFortuneEventDoc(
    val age: Int,
    val description: String,
    val amount: Long,
    val iconUrl: Image
)

/**
 * 사용자가 테스트를 실행한 결과. fortuneSnapshot 으로 콘텐츠 변경에도 결과 불변 보장.
 * peakAmount 는 graphData 최댓값을 미리 계산해 랭킹(`ageGroup=all`) 정렬 인덱스로 사용한다.
 */
data class WealthFortuneResult(
    @BsonId val id: ObjectId? = null,
    val name: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val birthTime: BirthTime?,
    val fortuneId: ObjectId,
    val fortuneSnapshot: WealthFortune,
    val peakAmount: Long,
    val createdAt: Instant = Clock.System.now()
)

/**
 * 그룹. inviteCode 는 unique. memberResultIds 는 최대 50개.
 */
data class WealthFortuneGroup(
    @BsonId val id: ObjectId? = null,
    val name: String,
    val inviteCode: String,
    val memberResultIds: List<ObjectId> = emptyList(),
    val createdAt: Instant = Clock.System.now()
)

class WealthFortuneRepository(
    private val database: MongoDatabase
) {
    suspend fun ensureIndexes() {
        // $sample 만 쓰므로 별도 인덱스 불필요. 후속 조회용 fortuneType 인덱스만.
        try {
            database
                .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
                .createIndex(Indexes.ascending(WealthFortune::fortuneType.name))
        } catch (e: MongoException) {
            System.err.println("Unable to ensure WealthFortune indexes: $e")
        }
    }

    suspend fun count(): Long =
        database
            .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
            .countDocuments()

    suspend fun insertOne(wealthFortune: WealthFortune): BsonValue? {
        try {
            return database
                .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
                .insertOne(wealthFortune)
                .insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert WealthFortune: $e")
        }
        return null
    }

    suspend fun insertMany(wealthFortunes: List<WealthFortune>): Int {
        if (wealthFortunes.isEmpty()) return 0
        try {
            val result =
                database
                    .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
                    .insertMany(wealthFortunes)
            return result.insertedIds.size
        } catch (e: MongoException) {
            System.err.println("Unable to insert WealthFortune list: $e")
        }
        return 0
    }

    suspend fun findById(objectId: ObjectId): WealthFortune? =
        database
            .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
            .find(Filters.eq("_id", objectId))
            .firstOrNull()

    /** $sample aggregation 으로 무작위 1개 추출. */
    suspend fun retrieveRandom(): WealthFortune? =
        database
            .getCollection<WealthFortune>(WEALTH_FORTUNE_COLLECTION)
            .aggregate<WealthFortune>(listOf(Aggregates.sample(1)))
            .firstOrNull()

    companion object {
        const val WEALTH_FORTUNE_COLLECTION = "wealth_fortune"
    }
}

class WealthFortuneResultRepository(
    private val database: MongoDatabase
) {
    suspend fun ensureIndexes() {
        try {
            database
                .getCollection<WealthFortuneResult>(WEALTH_FORTUNE_RESULT_COLLECTION)
                .createIndex(Indexes.descending(WealthFortuneResult::peakAmount.name))
        } catch (e: MongoException) {
            System.err.println("Unable to ensure WealthFortuneResult indexes: $e")
        }
    }

    suspend fun insertOne(result: WealthFortuneResult): BsonValue? {
        try {
            return database
                .getCollection<WealthFortuneResult>(WEALTH_FORTUNE_RESULT_COLLECTION)
                .insertOne(result)
                .insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert WealthFortuneResult: $e")
        }
        return null
    }

    suspend fun findById(objectId: ObjectId): WealthFortuneResult? =
        database
            .getCollection<WealthFortuneResult>(WEALTH_FORTUNE_RESULT_COLLECTION)
            .find(Filters.eq("_id", objectId))
            .firstOrNull()

    suspend fun findByIds(ids: List<ObjectId>): List<WealthFortuneResult> {
        if (ids.isEmpty()) return emptyList()
        return database
            .getCollection<WealthFortuneResult>(WEALTH_FORTUNE_RESULT_COLLECTION)
            .find(Filters.`in`("_id", ids))
            .toList()
    }

    suspend fun count(): Long =
        database
            .getCollection<WealthFortuneResult>(WEALTH_FORTUNE_RESULT_COLLECTION)
            .countDocuments()

    companion object {
        const val WEALTH_FORTUNE_RESULT_COLLECTION = "wealth_fortune_result"
    }
}

class WealthFortuneGroupRepository(
    private val database: MongoDatabase
) {
    suspend fun ensureIndexes() {
        try {
            database
                .getCollection<WealthFortuneGroup>(WEALTH_FORTUNE_GROUP_COLLECTION)
                .createIndex(
                    Indexes.ascending(WealthFortuneGroup::inviteCode.name),
                    IndexOptions().unique(true)
                )
        } catch (e: MongoException) {
            System.err.println("Unable to ensure WealthFortuneGroup indexes: $e")
        }
    }

    suspend fun insertOne(group: WealthFortuneGroup): BsonValue? {
        try {
            return database
                .getCollection<WealthFortuneGroup>(WEALTH_FORTUNE_GROUP_COLLECTION)
                .insertOne(group)
                .insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert WealthFortuneGroup: $e")
        }
        return null
    }

    suspend fun findById(objectId: ObjectId): WealthFortuneGroup? =
        database
            .getCollection<WealthFortuneGroup>(WEALTH_FORTUNE_GROUP_COLLECTION)
            .find(Filters.eq("_id", objectId))
            .firstOrNull()

    suspend fun findByInviteCode(inviteCode: String): WealthFortuneGroup? =
        database
            .getCollection<WealthFortuneGroup>(WEALTH_FORTUNE_GROUP_COLLECTION)
            .find(Filters.eq(WealthFortuneGroup::inviteCode.name, inviteCode))
            .firstOrNull()

    suspend fun existsByInviteCode(inviteCode: String): Boolean = findByInviteCode(inviteCode) != null

    /**
     * 그룹에 멤버 추가. 정원(maxMembers) 미만일 때만 atomic 하게 추가하고, 중복 추가는 방지($addToSet).
     * @return modifiedCount (1 이면 추가 성공, 0 이면 정원 초과 또는 이미 존재)
     */
    suspend fun addMemberIfNotFull(
        groupId: ObjectId,
        resultId: ObjectId,
        maxMembers: Int
    ): Long {
        try {
            val filter =
                Filters.and(
                    Filters.eq("_id", groupId),
                    Filters.expr(
                        org.bson.Document(
                            "\$lt",
                            listOf(
                                org.bson.Document(
                                    "\$size",
                                    org.bson.Document(
                                        "\$ifNull",
                                        listOf("\$${WealthFortuneGroup::memberResultIds.name}", emptyList<Any>())
                                    )
                                ),
                                maxMembers
                            )
                        )
                    )
                )
            val update = Updates.addToSet(WealthFortuneGroup::memberResultIds.name, resultId)
            val result =
                database
                    .getCollection<WealthFortuneGroup>(WEALTH_FORTUNE_GROUP_COLLECTION)
                    .updateOne(filter, update)
            return result.modifiedCount
        } catch (e: MongoException) {
            System.err.println("Unable to add member to WealthFortuneGroup: $e")
        }
        return 0
    }

    companion object {
        const val WEALTH_FORTUNE_GROUP_COLLECTION = "wealth_fortune_group"
    }
}