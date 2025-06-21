package com.mashup.dhc.domain.model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.bson.types.ObjectId
import org.junit.Test

class PastRoutineHistoryRepositoryTest : BaseMongoDBTest() {
    private lateinit var pastRoutineHistoryRepository: PastRoutineHistoryRepository

    override fun setUp() {
        // Setup repository
        pastRoutineHistoryRepository = PastRoutineHistoryRepository(database)
    }

    @Test
    fun testInsertAndFindMissionHistoryById() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val userId = ObjectId()
                val today = now()
                val pastRoutineHistory =
                    PastRoutineHistory(
                        id = null,
                        userId = userId,
                        date = today,
                        missions = emptyList()
                    )

                // When
                val insertedId = pastRoutineHistoryRepository.insertOne(pastRoutineHistory, session)
                assertNotNull(insertedId)

                val objectId = insertedId.asObjectId().value
                val foundMissionHistory = pastRoutineHistoryRepository.findById(objectId)

                // Then
                assertNotNull(foundMissionHistory)
                assertEquals(userId, foundMissionHistory.userId)
                assertEquals(today, foundMissionHistory.date)

                session.close()
            }
        }
    }

    @Test
    fun testFindMissionHistoriesByUserId() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val userId1 = ObjectId()
                val userId2 = ObjectId()
                val today = now()
                val yesterday = now().minus(1, DateTimeUnit.DAY)
                val twoDaysAgo = now().minus(2, DateTimeUnit.DAY)

                // Create multiple mission histories for userId1
                val history1 = PastRoutineHistory(id = null, userId = userId1, date = today, missions = emptyList())
                val history2 = PastRoutineHistory(id = null, userId = userId1, date = yesterday, missions = emptyList())

                // Create one mission history for userId2
                val history3 =
                    PastRoutineHistory(id = null, userId = userId2, date = twoDaysAgo, missions = emptyList())

                // Insert all mission histories
                pastRoutineHistoryRepository.insertOne(history1, session)
                pastRoutineHistoryRepository.insertOne(history2, session)
                pastRoutineHistoryRepository.insertOne(history3, session)

                // When
                val userId1Histories = pastRoutineHistoryRepository.findSortedByUserId(userId1)
                val userId2Histories = pastRoutineHistoryRepository.findSortedByUserId(userId2)

                // Then
                assertEquals(2, userId1Histories.size)
                assertEquals(1, userId2Histories.size)

                // Verify userId1 histories
                assertTrue(userId1Histories.any { it.date == today })
                assertTrue(userId1Histories.any { it.date == yesterday })
                assertTrue(userId1Histories.all { it.userId == userId1 })

                // Verify userId2 histories
                assertEquals(twoDaysAgo, userId2Histories.first().date)
                assertEquals(userId2, userId2Histories.first().userId)

                session.close()
            }
        }
    }

    @Test
    fun testFindMissionHistoryByUserIdAndDate() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val userId1 = ObjectId()
                val userId2 = ObjectId()
                val today = now()
                val yesterday = today.minus(1, DateTimeUnit.DAY)

                // Create mission histories
                val history1 = PastRoutineHistory(id = null, userId = userId1, date = today, missions = emptyList())
                val history2 = PastRoutineHistory(id = null, userId = userId1, date = yesterday, missions = emptyList())
                val history3 = PastRoutineHistory(id = null, userId = userId2, date = today, missions = emptyList())

                // Insert all mission histories
                pastRoutineHistoryRepository.insertOne(history1, session)
                pastRoutineHistoryRepository.insertOne(history2, session)
                pastRoutineHistoryRepository.insertOne(history3, session)

                // When
                val user1TodayHistory = pastRoutineHistoryRepository.findByUserIdAndDate(userId1, today, session)
                val user1YesterdayHistory =
                    pastRoutineHistoryRepository.findByUserIdAndDate(
                        userId1,
                        yesterday,
                        session
                    )
                val user2TodayHistory = pastRoutineHistoryRepository.findByUserIdAndDate(userId2, today, session)
                val nonExistingHistory = pastRoutineHistoryRepository.findByUserIdAndDate(userId2, yesterday, session)

                // Then
                assertNotNull(user1TodayHistory)
                assertEquals(userId1, user1TodayHistory.userId)
                assertEquals(today, user1TodayHistory.date)

                assertNotNull(user1YesterdayHistory)
                assertEquals(userId1, user1YesterdayHistory.userId)
                assertEquals(yesterday, user1YesterdayHistory.date)

                assertNotNull(user2TodayHistory)
                assertEquals(userId2, user2TodayHistory.userId)
                assertEquals(today, user2TodayHistory.date)

                // This should be null as there's no record for userId2 on yesterday
                assertNull(nonExistingHistory)

                session.close()
            }
        }
    }

    private fun now() =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
}