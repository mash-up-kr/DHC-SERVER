package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.Money
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoDatabase
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
    val cost: Money,
    val endDate: LocalDate?
)

enum class MissionCategory {
    TRANSPORTATION,
    FOOD,
    DIGITAL,
    SHOPPING,
    TRAVEL,
    SOCIAL
}

enum class MissionType {
    LONG_TERM,
    DAILY
}

class MissionRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun findById(missionId: ObjectId): Mission? =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(org.bson.Document("_id", missionId))
            .firstOrNull()

    suspend fun findLongTermByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.LONG_TERM)))
            .toList()

    suspend fun findDailyByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.DAILY)))
            .toList()

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
        .reduce(Money::plus)