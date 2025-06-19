package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.utils.Money
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.Before
import org.junit.Test

class MissionPickerTest {
    @MockK
    private lateinit var missionRepository: MissionRepository

    private lateinit var missionPicker: MissionPicker

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        missionPicker = MissionPicker(missionRepository)
    }

    @Test
    fun `pickMission returns mission from preferred category`() =
        runBlocking {
            // Given
            val preferredCategories = listOf(MissionCategory.FOOD)
            val expectedMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439011"),
                    category = MissionCategory.FOOD,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    cost = Money(150),
                    endDate = null,
                    title = "밥 먹기"
                )

            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.FOOD) } } returns
                listOf(expectedMission)

            // When
            val result = missionPicker.pickMission(preferredCategories)

            // Then
            assertEquals(expectedMission, result)
            verify { runBlocking { missionRepository.findDailyByCategory(MissionCategory.FOOD) } }
        }

    @Test
    fun `pickMission returns random mission from multiple missions in category`() =
        runBlocking {
            // Given
            val preferredCategories = listOf(MissionCategory.TRAVEL, MissionCategory.SHOPPING)
            val travelMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439011"),
                    category = MissionCategory.TRAVEL,
                    difficulty = 1,
                    type = MissionType.DAILY,
                    cost = Money(100),
                    endDate = null,
                    title = "밥 먹기"
                )
            val shoppingMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439022"),
                    category = MissionCategory.SHOPPING,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    cost = Money(200),
                    endDate = null,
                    title = "밥 먹기"
                )

            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.TRAVEL) } } returns
                listOf(travelMission)

            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.SHOPPING) } } returns
                listOf(shoppingMission)

            // When
            val actual =
                (0..10)
                    .map { missionPicker.pickMission(preferredCategories).category }
                    .toSet()

            // Then
            assertTrue(actual.size > 1)
        }

    @Test
    fun `pickMission with multiple preferred categories picks from random category`() =
        runBlocking {
            // Given
            val preferredCategories = listOf(MissionCategory.FOOD, MissionCategory.DIGITAL, MissionCategory.SHOPPING)

            val foodMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439011"),
                    category = MissionCategory.FOOD,
                    difficulty = 1,
                    type = MissionType.DAILY,
                    cost = Money(100),
                    endDate = null,
                    title = "밥 먹기"
                )
            val digitalMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439022"),
                    category = MissionCategory.DIGITAL,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    cost = Money(200),
                    endDate = null,
                    title = "밥 먹기"
                )
            val shoppingMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439033"),
                    category = MissionCategory.SHOPPING,
                    difficulty = 3,
                    type = MissionType.DAILY,
                    cost = Money(300),
                    endDate = null,
                    title = "밥 먹기"
                )

            // Mock all possible category calls
            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.FOOD) } } returns
                listOf(foodMission)
            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.DIGITAL) } } returns
                listOf(digitalMission)
            every { runBlocking { missionRepository.findDailyByCategory(MissionCategory.SHOPPING) } } returns
                listOf(shoppingMission)

            // When
            val result = missionPicker.pickMission(preferredCategories)

            // Then
            assertTrue(
                preferredCategories.contains(result.category),
                "Result category should be one of the preferred categories"
            )

            // Verify that one of the repository methods was called
            verify(atLeast = 1) {
                runBlocking {
                    missionRepository.findDailyByCategory(any())
                }
            }
        }
}