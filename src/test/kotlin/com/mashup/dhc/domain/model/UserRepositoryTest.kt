package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.CalendarType
import com.mashup.dhc.utils.Money
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.bson.types.ObjectId
import org.junit.Test

class UserRepositoryTest : BaseMongoDBTest() {
    private lateinit var userRepository: UserRepository

    override fun setUp() {
        // Setup repository
        userRepository = UserRepository(database)
    }

    @Test
    fun testInsertAndFindUser() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val user =
                    User(
                        id = null,
                        gender = Gender.MALE,
                        userToken = "test-token",
                        birthDate =
                            BirthDate(
                                LocalDate(2000, 1, 1),
                                CalendarType.SOLAR
                            ),
                        birthTime = null,
                        longTermMission =
                            Mission(
                                id = null,
                                category = MissionCategory.FOOD,
                                difficulty = 3,
                                title = "밥 먹기",
                                type = MissionType.DAILY,
                                cost = Money("10.00"),
                                endDate = null
                            ),
                        todayDailyMissionList =
                            listOf(
                                Mission(
                                    id = null,
                                    category = MissionCategory.FOOD,
                                    difficulty = 3,
                                    title = "밥 먹기",
                                    type = MissionType.DAILY,
                                    cost = Money("10.00"),
                                    endDate = null
                                )
                            ),
                        pastRoutineHistoryIds = emptyList(),
                        preferredMissionCategoryList = listOf(MissionCategory.FOOD),
                        currentAmulet =
                            Amulet(
                                totalPiece = 0,
                                remainPiece = 0
                            )
                    )

                // When
                val insertedId = userRepository.insertOne(user, session)
                assertNotNull(insertedId)

                val objectId = insertedId.asObjectId().value
                val foundUser = userRepository.findById(objectId)

                // Then
                assertNotNull(foundUser)
                assertEquals(Gender.MALE, foundUser.gender)
                assertEquals("test-token", foundUser.userToken)
                assertEquals(LocalDate(2000, 1, 1), foundUser.birthDate.date)
                assertEquals(CalendarType.SOLAR, foundUser.birthDate.calendarType)
                assertEquals(MissionCategory.FOOD, foundUser.longTermMission!!.category)
                assertEquals(1, foundUser.todayDailyMissionList.size)
                assertEquals(MissionCategory.FOOD, foundUser.todayDailyMissionList[0].category)

                session.close()
            }
        }
    }

    @Test
    fun testUpdateUser() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val user =
                    User(
                        id = null,
                        gender = Gender.MALE,
                        userToken = "test-token",
                        birthDate = BirthDate(LocalDate(2000, 1, 1), CalendarType.SOLAR),
                        birthTime = null,
                        longTermMission =
                            Mission(
                                id = null,
                                category = MissionCategory.FOOD,
                                difficulty = 3,
                                type = MissionType.DAILY,
                                cost = Money("10.00"),
                                title = "밥 먹기",
                                endDate = null
                            ),
                        todayDailyMissionList =
                            listOf(
                                Mission(
                                    id = null,
                                    category = MissionCategory.FOOD,
                                    difficulty = 3,
                                    type = MissionType.DAILY,
                                    cost = Money("10.00"),
                                    title = "밥 먹기",
                                    endDate = null
                                )
                            ),
                        pastRoutineHistoryIds = emptyList(),
                        preferredMissionCategoryList = listOf(MissionCategory.FOOD),
                        currentAmulet =
                            Amulet(
                                totalPiece = 0,
                                remainPiece = 0
                            )
                    )

                // When
                val insertedId = userRepository.insertOne(user, session)
                assertNotNull(insertedId)
                val objectId = insertedId.asObjectId().value

                val updatedUser =
                    User(
                        id = objectId,
                        gender = Gender.MALE,
                        userToken = "test-token",
                        birthDate = BirthDate(LocalDate(2000, 1, 1), CalendarType.SOLAR),
                        birthTime = null,
                        longTermMission =
                            Mission(
                                id = null,
                                category = MissionCategory.FOOD,
                                difficulty = 3,
                                type = MissionType.DAILY,
                                cost = Money("10.00"),
                                title = "밥 먹기",
                                endDate = null
                            ),
                        todayDailyMissionList =
                            listOf(
                                Mission(
                                    id = null,
                                    category = MissionCategory.TRANSPORTATION, // Changed category
                                    difficulty = 5, // Changed difficulty
                                    type = MissionType.DAILY,
                                    cost = Money("15.00"), // Changed cost
                                    title = "밥 먹기",
                                    endDate = null
                                )
                            ),
                        pastRoutineHistoryIds = listOf(ObjectId()), // Added mission history
                        preferredMissionCategoryList = listOf(MissionCategory.FOOD),
                        currentAmulet =
                            Amulet(
                                totalPiece = 0,
                                remainPiece = 0
                            )
                    )

                val updateCount = userRepository.updateOne(objectId, updatedUser, session)
                assertEquals(1, updateCount)

                // Then
                val foundUser = userRepository.findById(objectId)
                assertNotNull(foundUser)
                assertEquals(MissionCategory.TRANSPORTATION, foundUser.todayDailyMissionList[0].category)
                assertEquals(5, foundUser.todayDailyMissionList[0].difficulty)
                assertEquals(Money("15.00"), foundUser.todayDailyMissionList[0].cost)
                assertEquals(1, foundUser.pastRoutineHistoryIds.size)

                session.close()
            }
        }
    }

    @Test
    fun testDeleteUser() {
        runBlocking {
            mongoClient.startSession().let { session ->
                // Given
                val user =
                    User(
                        id = null,
                        gender = Gender.MALE,
                        userToken = "test-token",
                        birthDate = BirthDate(LocalDate(2000, 1, 1), CalendarType.SOLAR),
                        birthTime = null,
                        longTermMission =
                            Mission(
                                id = null,
                                category = MissionCategory.FOOD,
                                difficulty = 3,
                                type = MissionType.DAILY,
                                cost = Money("10.00"),
                                title = "밥 먹기",
                                endDate = null
                            ),
                        todayDailyMissionList = emptyList(),
                        pastRoutineHistoryIds = emptyList(),
                        preferredMissionCategoryList = listOf(MissionCategory.FOOD),
                        currentAmulet =
                            Amulet(
                                totalPiece = 0,
                                remainPiece = 0
                            )
                    )

                // When
                val insertedId = userRepository.insertOne(user, session)
                assertNotNull(insertedId)
                val objectId = insertedId.asObjectId().value

                val deleteCount = userRepository.deleteById(objectId, session)
                assertEquals(1, deleteCount)

                // Then
                val foundUser = userRepository.findById(objectId)
                assertNull(foundUser)

                session.close()
            }
        }
    }
}