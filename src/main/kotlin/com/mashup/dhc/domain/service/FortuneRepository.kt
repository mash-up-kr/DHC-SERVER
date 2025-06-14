package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository.Companion.USER_COLLECTION
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase

class FortuneRepository(
    private val database: MongoDatabase
) {


    suspend fun pushMonthlyFortune(userId: String, monthlyFortune: MonthlyFortune) {
        database.getCollection<User>(USER_COLLECTION)
            .updateOne(
                Filters.eq("_id", userId),
                Updates.push(User::monthlyFortuneList.name, monthlyFortune)
            )
    }

} 