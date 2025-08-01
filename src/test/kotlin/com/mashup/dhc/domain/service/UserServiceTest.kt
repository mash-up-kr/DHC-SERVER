package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.Amulet
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.CalendarType
import com.mashup.dhc.utils.Money
import com.mongodb.kotlin.client.coroutine.ClientSession
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    @MockK
    private lateinit var session: ClientSession

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
                    cost = Money(100),
                    endDate = null,
                    title = "밥 먹기"
                )

            val randomMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439033"),
                    category = missionCategory,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    cost = Money(200),
                    endDate = null,
                    title = "밥 먹기"
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
                            cost = Money(300),
                            endDate = null,
                            title = "밥 먹기"
                        ),
                    todayDailyMissionList = listOf(missionToReplace),
                    pastRoutineHistoryIds = listOf(),
                    preferredMissionCategoryList = listOf(missionCategory),
                    currentAmulet =
                        Amulet(
                            totalPiece = 0,
                            remainPiece = 0
                        ),
                    dailyFortune =
                        com.mashup.dhc.domain.model.DailyFortune(
                            date = "2025-01-01",
                            fortuneTitle = "테스트 운세",
                            fortuneDetail = "테스트 상세",
                            jinxedColor = "빨간색",
                            jinxedColorHex = "#FF0000",
                            jinxedMenu = "매운음식",
                            jinxedNumber = 13,
                            luckyColor = "파란색",
                            luckyColorHex = "#0000FF",
                            luckyNumber = 7,
                            positiveScore = 70,
                            negativeScore = 20,
                            todayMenu = "과일"
                        )
                )

            // Mock transaction service to execute the block directly
            coEvery { transactionService.executeInTransaction<User>(any()) } coAnswers {
                val block = arg<suspend (ClientSession) -> User>(0)
                block(session)
            }

            // Mock repository calls
            every { runBlocking { userRepository.findById(ObjectId(userId), session) } } returns user
            every {
                runBlocking {
                    missionRepository.findByCategory(
                        MissionType.DAILY,
                        missionCategory,
                        session
                    )
                }
            } returns
                listOf(randomMission)
            every { runBlocking { userRepository.updateOne(ObjectId(userId), any(), session) } } returns 1

            // When
            val result = userService.switchTodayMission(userId, missionIdToReplace)

            // Verify the updated user has the correct missions
            val userSlot = slot<User>()
            verify { runBlocking { userRepository.updateOne(ObjectId(userId), capture(userSlot), session) } }

            val updatedMission = userSlot.captured.todayDailyMissionList[0]
            assertEquals(randomMission.id, updatedMission.id)
            assertEquals(randomMission.difficulty, updatedMission.difficulty)
            assertEquals(randomMission.type, updatedMission.type)
            assertEquals(randomMission.cost, updatedMission.cost)
            assertEquals(1, updatedMission.switchCount)
        }

    @Test
    fun `updateTodayMission updates longTermMission when missionId matches longTermMission`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val longTermMissionId = "507f1f77bcf86cd799439044"
            val finished = true

            val longTermMission =
                Mission(
                    id = ObjectId(longTermMissionId),
                    category = MissionCategory.TRAVEL,
                    difficulty = 5,
                    type = MissionType.LONG_TERM,
                    finished = false,
                    cost = Money(500),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission1 =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439055"),
                    category = MissionCategory.FOOD,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(100),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission2 =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439066"),
                    category = MissionCategory.TRANSPORTATION,
                    difficulty = 1,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(50),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission3 =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439077"),
                    category = MissionCategory.SHOPPING,
                    difficulty = 3,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(200),
                    endDate = null,
                    title = "밥 먹기"
                )

            val user =
                User(
                    id = ObjectId(userId),
                    gender = Gender.MALE,
                    userToken = "test-token",
                    birthDate = BirthDate(now(), CalendarType.SOLAR),
                    birthTime = null,
                    longTermMission = longTermMission,
                    todayDailyMissionList = listOf(dailyMission1, dailyMission2, dailyMission3),
                    pastRoutineHistoryIds = listOf(),
                    preferredMissionCategoryList = listOf(MissionCategory.TRAVEL),
                    currentAmulet =
                        Amulet(
                            totalPiece = 0,
                            remainPiece = 0
                        )
                )

            // Mock transaction service to execute the block directly
            coEvery { transactionService.executeInTransaction<User>(any()) } coAnswers {
                val block = arg<suspend (ClientSession) -> User>(0)
                block(session)
            }

            // Mock repository calls
            every { runBlocking { userRepository.findById(ObjectId(userId), session) } } returns user
            every { runBlocking { userRepository.updateOne(ObjectId(userId), any(), session) } } returns 1

            // When
            val result = userService.updateTodayMission(userId, longTermMissionId, finished)

            // Then
            // Verify longTermMission is updated
            assertTrue(result.longTermMission!!.finished)
            assertEquals(longTermMissionId, result.longTermMission!!.id.toString())

            // Verify dailyMissionList remains unchanged
            assertEquals(3, result.todayDailyMissionList.size)
            assertFalse(result.todayDailyMissionList[0].finished)
            assertFalse(result.todayDailyMissionList[1].finished)
            assertFalse(result.todayDailyMissionList[2].finished)
        }

    @Test
    fun `updateTodayMission updates second element of todayDailyMissionList when missionId matches`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val secondMissionId = "507f1f77bcf86cd799439066"
            val finished = true

            val longTermMission =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439044"),
                    category = MissionCategory.TRAVEL,
                    difficulty = 5,
                    type = MissionType.LONG_TERM,
                    finished = false,
                    cost = Money(500),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission1 =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439055"),
                    category = MissionCategory.FOOD,
                    difficulty = 2,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(100),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission2 =
                Mission(
                    id = ObjectId(secondMissionId),
                    category = MissionCategory.TRANSPORTATION,
                    difficulty = 1,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(50),
                    endDate = null,
                    title = "밥 먹기"
                )

            val dailyMission3 =
                Mission(
                    id = ObjectId("507f1f77bcf86cd799439077"),
                    category = MissionCategory.SHOPPING,
                    difficulty = 3,
                    type = MissionType.DAILY,
                    finished = false,
                    cost = Money(200),
                    endDate = null,
                    title = "밥 먹기"
                )

            val user =
                User(
                    id = ObjectId(userId),
                    gender = Gender.MALE,
                    userToken = "test-token",
                    birthDate = BirthDate(now(), CalendarType.SOLAR),
                    birthTime = null,
                    longTermMission = longTermMission,
                    todayDailyMissionList = listOf(dailyMission1, dailyMission2, dailyMission3),
                    pastRoutineHistoryIds = listOf(),
                    preferredMissionCategoryList = listOf(MissionCategory.TRANSPORTATION),
                    currentAmulet =
                        Amulet(
                            totalPiece = 0,
                            remainPiece = 0
                        )
                )

            // Mock transaction service to execute the block directly
            coEvery { transactionService.executeInTransaction<User>(any()) } coAnswers {
                val block = arg<suspend (ClientSession) -> User>(0)
                block(session)
            }

            // Mock repository calls
            every { runBlocking { userRepository.findById(ObjectId(userId), session) } } returns user
            every { runBlocking { userRepository.updateOne(ObjectId(userId), any(), session) } } returns 1

            // When
            val result = userService.updateTodayMission(userId, secondMissionId, finished)

            // Then
            // Verify longTermMission remains unchanged
            assertFalse(result.longTermMission!!.finished)

            // Verify only the second daily mission is updated
            assertEquals(3, result.todayDailyMissionList.size)
            assertFalse(result.todayDailyMissionList[0].finished) // First mission unchanged
            assertTrue(result.todayDailyMissionList[1].finished) // Second mission updated
            assertFalse(result.todayDailyMissionList[2].finished) // Third mission unchanged

            // Verify the updated mission details
            assertEquals(secondMissionId, result.todayDailyMissionList[1].id.toString())
        }

    private fun now() =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
}