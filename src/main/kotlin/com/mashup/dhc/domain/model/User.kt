package com.mashup.dhc.domain.model

import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    @BsonId val id: ObjectId?,
    val name: String,
    val gender: Gender,
    val userToken: String,
    val birthDate: LocalDate,
    val longTermMission: Mission,
    val todayDailyMissionList: List<Mission>,
    val pastRoutineHistoryIds: List<ObjectId>
)

class UserRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun insertOne(user: User): BsonValue? {
        try {
            val result =
                mongoDatabase.getCollection<User>(USER_COLLECTION).insertOne(
                    user
                )
            return result.insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert due to an error: $e")
        }
        return null
    }

    suspend fun deleteById(objectId: ObjectId): Long {
        try {
            val result = mongoDatabase.getCollection<User>(USER_COLLECTION).deleteOne(Filters.eq("_id", objectId))
            return result.deletedCount
        } catch (e: MongoException) {
            System.err.println("Unable to delete due to an error: $e")
        }
        return 0
    }

    suspend fun findById(objectId: ObjectId): User? =
        mongoDatabase
            .getCollection<User>(USER_COLLECTION)
            .find(Filters.eq("_id", objectId))
            .firstOrNull()

    suspend fun updateOne(
        objectId: ObjectId,
        user: User
    ): Long {
        try {
            val query = Filters.eq("_id", objectId)
            val updates =
                Updates.combine(
                    Updates.set(User::longTermMission.name, user.longTermMission),
                    Updates.set(User::todayDailyMissionList.name, user.todayDailyMissionList),
                    Updates.set(User::pastRoutineHistoryIds.name, user.pastRoutineHistoryIds)
                )
            val result =
                mongoDatabase
                    .getCollection<User>(USER_COLLECTION)
                    .updateOne(query, updates, UpdateOptions().upsert(false))
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