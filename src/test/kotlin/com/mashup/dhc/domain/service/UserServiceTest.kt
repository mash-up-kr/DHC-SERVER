package com.mashup.dhc.domain.service

import com.mashup.com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.com.mashup.dhc.domain.service.MissionPicker
import com.mashup.com.mashup.dhc.domain.service.UserService
import com.mashup.com.mashup.dhc.utils.BirthDate
import com.mashup.com.mashup.dhc.utils.CalendarType
import com.mashup.com.mashup.dhc.utils.Money
import com.mashup.dhc.domain.model.Amulet
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bson.types.ObjectId
import org.junit.Before
import org.junit.Test

class UserServiceTest {
    @MockK
    private lateinit var transactionService: TransactionService

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var missionRepository: MissionRepository

    @MockK
    private lateinit var pastRoutineHistoryRepository: PastRoutineHistoryRepository

    private lateinit var missionPicker: MissionPicker

    private lateinit var userService: UserService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        missionPicker = MissionPicker(missionRepository)
        userService =
            UserService(
                transactionService,
                userRepository,
                missionRepository,
                pastRoutineHistoryRepository,
                missionPicker
            )
    }

    @Test
    fun `switchTodayMission returns not always same category`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val missionIdToReplace = "507f1f77bcf86cd799439022"
            val missionCategory = MissionCategory.TRAVEL

            val missionToReplace =
                Mission(
                    id = ObjectId(missionIdToReplace),
                    category = missionCategory,
                    difficulty = 1,
                    type = MissionType.DAILY,
                    cost = Money(100)
                )

            val randomMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439033"),
                    category = missionCategory,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    cost = Money(200)
                )

            val user =
                User(
                    id = ObjectId(userId),
                    gender = Gender.MALE,
                    userToken = "test-token",
                    birthDate = BirthDate(now(), CalendarType.SOLAR),
                    birthTime = null,
                    longTermMission =
                        Mission(
                            id = ObjectId("507f1f77bcf86cd799439044"),
                            category = missionCategory,
                            difficulty = 3,
                            type = MissionType.DAILY,
                            cost = Money(300)
                        ),
                    todayDailyMissionList = listOf(missionToReplace),
                    pastRoutineHistoryIds = listOf(),
                    preferredMissionCategoryList = listOf(missionCategory),
                    currentAmulet =
                        Amulet(
                            totalPiece = 0,
                            remainPiece = 0
                        )
                )

            // Mock repository calls
            every { runBlocking { userRepository.findById(ObjectId(userId)) } } returns user
            every { runBlocking { missionRepository.findDailyByCategory(missionCategory) } } returns
                listOf(randomMission)
            every { runBlocking { userRepository.updateOne(ObjectId(userId), any()) } } returns 1

            // When
            val result = userService.switchTodayMission(userId, missionIdToReplace)

            // Then
            assertEquals(1, result)

            // Verify the updated user has the correct missions
            val userSlot = slot<User>()
            verify { runBlocking { userRepository.updateOne(ObjectId(userId), capture(userSlot)) } }

            val updatedMission = userSlot.captured.todayDailyMissionList[0]
            assertEquals(randomMission.id, updatedMission.id)
            assertEquals(randomMission.difficulty, updatedMission.difficulty)
            assertEquals(randomMission.type, updatedMission.type)
            assertEquals(randomMission.cost, updatedMission.cost)
        }

    private fun now() =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
}