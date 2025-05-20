package com.mashup.dhc.domain.service

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient

class TransactionService(
    private val mongoClient: MongoClient
) {
    suspend fun executeInTransaction(operations: suspend (ClientSession) -> Unit) {
        val session = mongoClient.startSession()

        try {
            session.startTransaction()
            operations(session)
            session.commitTransaction()
        } catch (e: Exception) {
            session.abortTransaction()
            throw e
        } finally {
            session.close()
        }
    }
}