package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.utils.Money
import com.mongodb.kotlin.client.coroutine.ClientSession
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

    @MockK
    private lateinit var session: ClientSession

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

            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.FOOD,
                        session
                    )
                }
            } returns
                listOf(expectedMission)

            // When - difficulty 2는 MIDDLE(34-67) 범위에 포함되도록 luckTotalScore를 50으로 설정
            val result =
                missionPicker.pickMission(
                    preferredMissionCategoryList = preferredCategories,
                    luckTotalScore = 50,
                    session = session
                )

            // Then
            assertEquals(expectedMission, result)
            verify {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.FOOD,
                        session
                    )
                }
            }
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

            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.TRAVEL,
                        session
                    )
                }
            } returns
                listOf(travelMission)

            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.SHOPPING,
                        session
                    )
                }
            } returns
                listOf(shoppingMission)

            // When - difficulty 1,2는 각각 EASY(0-33), MIDDLE(34-67) 범위에 포함되도록 점수 설정
            val actual =
                (0..10)
                    .map {
                        missionPicker
                            .pickMission(
                                preferredMissionCategoryList = preferredCategories,
                                luckTotalScore = if (it % 2 == 0) 20 else 50, // EASY와 MIDDLE 범위 번갈아가며
                                session = session
                            ).category
                    }.toSet()

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
            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.FOOD,
                        session
                    )
                }
            } returns
                listOf(foodMission)
            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.DIGITAL,
                        session
                    )
                }
            } returns
                listOf(digitalMission)
            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        MissionCategory.SHOPPING,
                        session
                    )
                }
            } returns
                listOf(shoppingMission)

            // When - 모든 difficulty 범위를 포함하는 점수로 설정
            val result =
                missionPicker.pickMission(
                    preferredMissionCategoryList = preferredCategories,
                    luckTotalScore = 50, // MIDDLE 범위에 포함되는 점수
                    session = session
                )

            // Then
            assertTrue(
                preferredCategories.contains(result.category),
                "Result category should be one of the preferred categories"
            )

            // Verify that one of the repository methods was called
            verify(atLeast = 1) {
                runBlocking {
                    missionRepository.findByCategory(MissionType.DAILY, any(), session)
                }
            }
        }
}