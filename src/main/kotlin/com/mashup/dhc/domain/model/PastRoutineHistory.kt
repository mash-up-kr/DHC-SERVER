package com.mashup.dhc.domain.model

import com.mongodb.MongoException
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import org.bson.BsonValue
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class PastRoutineHistory(
    @BsonId val id: ObjectId?,
    val userId: ObjectId,
    val date: LocalDate,
    val missions: List<Mission>
)

class PastRoutineHistoryRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun insertOne(
        pastRoutineHistory: PastRoutineHistory,
        session: ClientSession
    ): BsonValue? {
        try {
            val result =
                mongoDatabase.getCollection<PastRoutineHistory>(PAST_ROUTINE_HISTORY_COLLECTION).insertOne(
                    session,
                    pastRoutineHistory
                )
            return result.insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert due to an error: $e")
        }
        return null
    }

    suspend fun findById(objectId: ObjectId): PastRoutineHistory? =
        mongoDatabase
            .getCollection<PastRoutineHistory>(PAST_ROUTINE_HISTORY_COLLECTION)
            .find(eq("_id", objectId))
            .firstOrNull()

    suspend fun findSortedByUserId(userId: ObjectId): List<PastRoutineHistory> =
        mongoDatabase
            .getCollection<PastRoutineHistory>(PAST_ROUTINE_HISTORY_COLLECTION)
            .find(eq("userId", userId))
            .sort(Document("date", -1)) // 날짜를 기준으로 내림차순 정렬 (최신 날짜가 먼저)
            .toList()

    suspend fun findByUserIdAndDate(
        userId: ObjectId,
        date: LocalDate,
        session: ClientSession
    ): PastRoutineHistory? =
        mongoDatabase
            .getCollection<PastRoutineHistory>(PAST_ROUTINE_HISTORY_COLLECTION)
            .find(session, and(eq("userId", userId), eq("date", date)))
            .firstOrNull()

    suspend fun findByUserIdAndDateBetween(
        userId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PastRoutineHistory> =
        mongoDatabase
            .getCollection<PastRoutineHistory>(PAST_ROUTINE_HISTORY_COLLECTION)
            .find(and(eq("userId", userId), gte("date", startDate), lte("date", endDate)))
            .sort(Document("date", 1)) // 날짜를 기준으로 오름차순 정렬 (최신 날짜가 마지막)
            .toList()

    companion object {
        const val PAST_ROUTINE_HISTORY_COLLECTION = "past_routine_history"
    }
}