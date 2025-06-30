package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.Money
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.math.BigDecimal
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Mission(
    @BsonId val id: ObjectId?,
    val category: MissionCategory,
    val difficulty: Int,
    val type: MissionType,
    val finished: Boolean = false,
    val title: String,
    val cost: Money,
    val switchCount: Int = 0,
    val endDate: LocalDate?
)

enum class MissionCategory(
    val displayName: String,
    val imageUrl: String
) {
    TRANSPORTATION("이동·교통", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/transportaion.png"),
    FOOD("식음료", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/food.png"),
    DIGITAL("디지털·구독", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/digital.png"),
    SHOPPING("쇼핑", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/shopping.png"),
    TRAVEL("취미·문화", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/hobby.png"),
    SOCIAL("사교·모임", "https://dhc-object-storage.kr.object.ncloudstorage.com/logos/friend.png"),
    SELF_REFLECTION("회고", "")
}

enum class MissionType {
    LONG_TERM,
    DAILY
}

class MissionRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun findById(
        missionId: ObjectId,
        session: ClientSession
    ): Mission? =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(session, org.bson.Document("_id", missionId))
            .firstOrNull()

    suspend fun findLongTermByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.LONG_TERM)))
            .toList()

    suspend fun findLongTermByCategory(
        category: MissionCategory,
        session: ClientSession
    ): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(session, and(eq("category", category), eq("type", MissionType.LONG_TERM)))
            .toList()

    suspend fun findDailyByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.DAILY)))
            .toList()

    suspend fun findByCategory(
        type: MissionType,
        category: MissionCategory,
        session: ClientSession
    ): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(
                session,
                and(
                    or(eq("category", category), eq("category", MissionCategory.SELF_REFLECTION)),
                    eq("type", type)
                )
            ).toList()

    suspend fun findAll(): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find()
            .toList()

    companion object {
        const val MISSION_COLLECTION = "mission"
    }
}

fun List<Mission>.calculateSavedMoney() =
    this
        .filter { it.finished }
        .map { it.cost }
        .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)