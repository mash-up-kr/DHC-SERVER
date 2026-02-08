package com.mashup.dhc.domain.service

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient

class TransactionService(
    private val mongoClient: MongoClient
) {
    suspend fun <T> executeInTransaction(operations: suspend (ClientSession) -> T): T {
        val session = mongoClient.startSession()

        return session.use { activeSession ->
            activeSession.startTransaction()
            try {
                val result = operations(activeSession)
                activeSession.commitTransaction()
                result
            } catch (e: Exception) {
                activeSession.abortTransaction()
                throw e
            }
        }
    }
}