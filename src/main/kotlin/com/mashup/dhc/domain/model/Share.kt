package com.mashup.dhc.domain.model

import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Share(
    @BsonId val id: ObjectId? = null,
    val shareCode: String,
    val userId: ObjectId,
    val completed: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null
) {
    companion object {
        fun create(userId: ObjectId): Share =
            Share(
                shareCode = UUID.randomUUID().toString().replace("-", "").take(12),
                userId = userId
            )
    }
}

class ShareRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun insertOne(share: Share): BsonValue? {
        try {
            val result =
                mongoDatabase
                    .getCollection<Share>(SHARE_COLLECTION)
                    .insertOne(share)
            return result.insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert share due to an error: $e")
        }
        return null
    }

    suspend fun findByShareCode(shareCode: String): Share? =
        mongoDatabase
            .getCollection<Share>(SHARE_COLLECTION)
            .find(Filters.eq("shareCode", shareCode))
            .firstOrNull()

    suspend fun findByUserId(userId: ObjectId): List<Share> {
        val shares = mutableListOf<Share>()
        mongoDatabase
            .getCollection<Share>(SHARE_COLLECTION)
            .find(Filters.eq("userId", userId))
            .collect { shares.add(it) }
        return shares
    }

    suspend fun hasCompletedShare(userId: ObjectId): Boolean =
        mongoDatabase
            .getCollection<Share>(SHARE_COLLECTION)
            .find(
                Filters.and(
                    Filters.eq("userId", userId),
                    Filters.eq("completed", true)
                )
            )
            .firstOrNull() != null

    suspend fun markAsCompleted(shareCode: String): Long {
        try {
            val query = Filters.eq("shareCode", shareCode)
            val updates =
                Updates.combine(
                    Updates.set(Share::completed.name, true),
                    Updates.set(Share::completedAt.name, Clock.System.now())
                )
            val result =
                mongoDatabase
                    .getCollection<Share>(SHARE_COLLECTION)
                    .updateOne(query, updates)
            return result.modifiedCount
        } catch (e: MongoException) {
            System.err.println("Unable to update share due to an error: $e")
        }
        return 0
    }

    companion object {
        const val SHARE_COLLECTION = "share"
    }
}
