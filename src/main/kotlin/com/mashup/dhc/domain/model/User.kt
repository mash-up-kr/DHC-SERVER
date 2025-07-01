package com.mashup.dhc.domain.model

import com.mashup.dhc.domain.service.now
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Money
import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.math.BigDecimal
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Transient
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    @BsonId val id: ObjectId? = null,
    val gender: Gender,
    val userToken: String,
    val birthDate: BirthDate,
    val birthTime: BirthTime?,
    val preferredMissionCategoryList: List<MissionCategory>,
    val longTermMission: Mission? = null,
    val todayDailyMissionList: List<Mission> = listOf(),
    val pastRoutineHistoryIds: List<ObjectId> = listOf(),
    val monthlyFortune: MonthlyFortune? = null,
    val currentAmulet: Amulet? = null,
    val totalSavedMoney: Money = Money(BigDecimal.ZERO),
    @Transient val deleted: Boolean = false
) {
    private val age: Int
        get() = now().year - birthDate.date.year + 1

    val generation: Generation
        get() {
            return when (age) {
                in 10..19 -> Generation.TEENAGERS
                in 20..29 -> Generation.TWENTIES
                in 30..39 -> Generation.THIRTIES
                in 40..99 -> Generation.FORTIES
                else -> Generation.UNKNOWN
            }
        }
}

data class Amulet(
    val totalPiece: Int,
    val remainPiece: Int
)

class UserRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun insertOne(
        user: User,
        session: ClientSession
    ): BsonValue? {
        try {
            val result =
                mongoDatabase.getCollection<User>(USER_COLLECTION).insertOne(
                    session,
                    user
                )
            return result.insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert due to an error: $e")
        }
        return null
    }

    suspend fun deleteById(
        objectId: ObjectId,
        session: ClientSession
    ): Long {
        try {
            val result =
                mongoDatabase
                    .getCollection<User>(USER_COLLECTION)
                    .updateOne(session, Filters.eq("_id", objectId), Updates.set(User::deleted.name, true))
            return result.modifiedCount
        } catch (e: MongoException) {
            System.err.println("Unable to delete due to an error: $e")
        }
        return 0
    }

    suspend fun findById(objectId: ObjectId): User? =
        mongoDatabase
            .getCollection<User>(USER_COLLECTION)
            .find(Filters.and(Filters.eq("_id", objectId), Filters.eq("deleted", false)))
            .firstOrNull()

    suspend fun findById(
        objectId: ObjectId,
        session: ClientSession
    ): User? =
        mongoDatabase
            .getCollection<User>(USER_COLLECTION)
            .find(session, Filters.and(Filters.eq("_id", objectId), Filters.eq("deleted", false)))
            .firstOrNull()

    suspend fun findByUserToken(userToken: String): User? =
        mongoDatabase
            .getCollection<User>(USER_COLLECTION)
            .find(Filters.and(Filters.eq("userToken", userToken), Filters.eq("deleted", false)))
            .firstOrNull()

    suspend fun findByUserToken(
        userToken: String,
        session: ClientSession
    ): User? =
        mongoDatabase
            .getCollection<User>(USER_COLLECTION)
            .find(session, Filters.and(Filters.eq("userToken", userToken), Filters.eq("deleted", false)))
            .firstOrNull()

    suspend fun updateOne(
        objectId: ObjectId,
        user: User,
        session: ClientSession
    ): Long {
        try {
            val query = Filters.eq("_id", objectId)
            val updates =
                Updates.combine(
                    Updates.set(User::longTermMission.name, user.longTermMission),
                    Updates.set(User::todayDailyMissionList.name, user.todayDailyMissionList),
                    Updates.set(User::pastRoutineHistoryIds.name, user.pastRoutineHistoryIds),
                    Updates.set(User::totalSavedMoney.name, user.totalSavedMoney)
                )
            val result =
                mongoDatabase
                    .getCollection<User>(USER_COLLECTION)
                    .updateOne(session, query, updates, UpdateOptions().upsert(false))
            return result.modifiedCount
        } catch (e: MongoException) {
            System.err.println("Unable to update due to an error: $e")
        }
        return 0
    }

    companion object {
        const val USER_COLLECTION = "user"
    }
}

enum class Gender {
    MALE,
    FEMALE
}

enum class Generation(
    val description: String
) {
    TEENAGERS("10대"),
    TWENTIES("20대"),
    THIRTIES("30대"),
    FORTIES("40대"),
    UNKNOWN("알 수 없음")
}