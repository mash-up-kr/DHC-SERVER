package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.CalendarType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bson.types.ObjectId
import org.junit.After
import org.junit.Before
import org.junit.Test

class FortuneServiceTest {
    @MockK
    private lateinit var userService: UserService

    @MockK
    private lateinit var fortuneRepository: FortuneRepository

    @MockK
    private lateinit var geminiService: GeminiService

    private lateinit var fortuneService: FortuneService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(LockRegistry)
        fortuneService =
            FortuneService(
                backgroundScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
                userService = userService,
                fortuneRepository = fortuneRepository,
                geminiService = geminiService
            )
    }

    @After
    fun tearDown() {
        unmockkObject(LockRegistry)
    }

    @Test
    fun `운세 생성 작업 추가 시 큐에 성공적으로 추가되어야 한다`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val requestDate = LocalDate.of(2024, 3, 15)
            val lockKey = "$userId-2024-3"
            val user = createTestUser(userId)

            coEvery { LockRegistry.tryLock(lockKey) } returns true
            coEvery { userService.getUserById(userId) } returns user

            // When
            fortuneService.addFortuneGenerationTask(userId, requestDate)

            // Then
            assertEquals(1, fortuneService.getPendingRequestCount())
            coVerify { LockRegistry.tryLock(lockKey) }
        }

    @Test
    fun `락 획득 실패 시 예외를 발생시켜야 한다`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val requestDate = LocalDate.of(2024, 3, 15)
            val lockKey = "$userId-2024-3"

            coEvery { LockRegistry.tryLock(lockKey) } returns false

            // When & Then
            assertFailsWith<RuntimeException> {
                fortuneService.addFortuneGenerationTask(userId, requestDate)
            }

            coVerify { LockRegistry.tryLock(lockKey) }
        }

    @Test
    fun `이미 운세가 존재할 때 예외를 발생시켜야 한다`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val requestDate = LocalDate.of(2024, 3, 15)
            val lockKey = "$userId-2024-3"
            val user = createUserWithExistingFortune(userId, requestDate)

            coEvery { LockRegistry.tryLock(lockKey) } returns true
            coEvery { LockRegistry.unlock(lockKey) } returns Unit
            coEvery { userService.getUserById(userId) } returns user

            // When & Then
            assertFailsWith<RuntimeException> {
                fortuneService.addFortuneGenerationTask(userId, requestDate)
            }

            coVerify { LockRegistry.unlock(lockKey) }
        }

    @Test
    fun `운세가 존재할 때 올바른 운세를 반환해야 한다`() =
        runBlocking {
            // Given
            val userId = "507f1f77bcf86cd799439011"
            val requestDate = LocalDate.of(2024, 3, 15)
            val expectedFortune =
                DailyFortune(
                    date = "2024-03-15",
                    fortuneTitle = "좋은 운세",
                    fortuneDetail = "오늘은 좋은 일이 있을 것입니다",
                    jinxedColor = "빨간색",
                    jinxedColorHex = "#FF0000",
                    jinxedMenu = "매운음식",
                    jinxedNumber = 4,
                    luckyColor = "파란색",
                    luckyColorHex = "#0000FF",
                    luckyNumber = 7,
                    positiveScore = 80,
                    negativeScore = 20,
                    todayMenu = "샐러드"
                )
            val user = createUserWithFortune(userId, requestDate, expectedFortune)

            coEvery { userService.getUserById(userId) } returns user

            // When
            val result = fortuneService.queryDailyFortune(userId, requestDate)

            // Then
            assertEquals(expectedFortune, result)
        }

    @Test
    fun `대기 중인 요청 수를 올바르게 반환해야 한다`() =
        runBlocking {
            // Given
            val userId1 = "507f1f77bcf86cd799439011"
            val userId2 = "507f1f77bcf86cd799439022"
            val requestDate = LocalDate.of(2024, 3, 15)
            val user = createTestUser(userId1)

            coEvery { LockRegistry.tryLock(any()) } returns true
            coEvery { userService.getUserById(any()) } returns user

            // When
            fortuneService.addFortuneGenerationTask(userId1, requestDate)
            fortuneService.addFortuneGenerationTask(userId2, requestDate)

            // Then
            assertEquals(2, fortuneService.getPendingRequestCount())
        }

    @Test
    fun `여러 작업이 큐에 순서대로 추가되어야 한다`() =
        runBlocking {
            // Given
            val requestDate = LocalDate.of(2024, 3, 15)
            val user = createTestUser("685faf0cde38af6c7bd9d24e")

            coEvery { LockRegistry.tryLock(any()) } returns true
            coEvery { userService.getUserById(any()) } returns user

            // When
            repeat(5) { index ->
                val userId = "user-$index"
                fortuneService.addFortuneGenerationTask(userId, requestDate)
            }

            // Then
            assertEquals(5, fortuneService.getPendingRequestCount())
        }

    private fun createTestUser(userId: String): User =
        User(
            id = ObjectId(userId),
            gender = Gender.MALE,
            userToken = "test-token",
            birthDate =
                BirthDate(
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date,
                    CalendarType.SOLAR
                ),
            birthTime = BirthTime(LocalTime(1, 1, 1)),
            longTermMission = null,
            todayDailyMissionList = listOf(),
            pastRoutineHistoryIds = listOf(),
            preferredMissionCategoryList = listOf(),
            monthlyFortune = null,
            currentAmulet = mockk()
        )

    private fun createUserWithExistingFortune(
        userId: String,
        requestDate: LocalDate
    ): User {
        val existingFortune =
            DailyFortune(
                date = requestDate.toString(),
                fortuneTitle = "기존 운세",
                fortuneDetail = "이미 존재하는 운세입니다",
                jinxedColor = "검은색",
                jinxedColorHex = "#000000",
                jinxedMenu = "기름진음식",
                jinxedNumber = 13,
                luckyColor = "빨간색",
                luckyColorHex = "#FF0000",
                luckyNumber = 8,
                positiveScore = 60,
                negativeScore = 40,
                todayMenu = "국밥"
            )

        val monthlyFortune =
            MonthlyFortune(
                month = requestDate.monthValue,
                year = requestDate.year,
                dailyFortuneList = listOf(existingFortune)
            )

        return createTestUser(userId).copy(monthlyFortune = monthlyFortune)
    }

    private fun createUserWithFortune(
        userId: String,
        requestDate: LocalDate,
        dailyFortune: DailyFortune
    ): User {
        val monthlyFortune =
            MonthlyFortune(
                month = requestDate.monthValue,
                year = requestDate.year,
                dailyFortuneList = listOf(dailyFortune)
            )

        return createTestUser(userId).copy(monthlyFortune = monthlyFortune)
    }
}