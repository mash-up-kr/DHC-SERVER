package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.FortuneCache
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class FortuneRepository(
    private val database: MongoDatabase
) {
    private val collection = database.getCollection<FortuneCache>("fortunes")

    suspend fun init() {
        // TTL 인덱스 생성 (expiresAt 필드 기준으로 자동 삭제)
        collection.createIndex(
            Indexes.ascending("expiresAt"),
            IndexOptions().expireAfter(0, TimeUnit.SECONDS)
        )

        // userId와 month, year 조합으로 복합 인덱스 생성
        collection.createIndex(
            Document()
                .append("userId", 1)
                .append("month", 1)
                .append("year", 1)
        )
    }

    suspend fun saveFortune(fortune: FortuneCache): FortuneCache {
        // 기존 캐시가 있다면 삭제
        collection.deleteMany(
            Filters.and(
                Filters.eq("userId", fortune.userId),
                Filters.eq("month", fortune.month),
                Filters.eq("year", fortune.year)
            )
        )

        // 새 캐시 저장
        collection.insertOne(fortune)
        return fortune
    }

    suspend fun getFortuneByMonth(userId: String, year: Int, month: Int): FortuneCache? {
        return collection.find(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("month", month),
                Filters.eq("year", year)
            )
        ).firstOrNull()
    }

    suspend fun deleteByUserIdAndMonth(userId: String, year: Int, month: Int): Boolean {
        val result = collection.deleteOne(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("month", month),
                Filters.eq("year", year)
            )
        )
        return result.deletedCount > 0
    }
} 