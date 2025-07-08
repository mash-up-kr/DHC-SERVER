package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.PastRoutineHistory
import com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.domain.model.calculateSavedMoney
import com.mashup.dhc.routes.BusinessException
import com.mashup.dhc.routes.ErrorCode
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Money
import com.mongodb.kotlin.client.coroutine.ClientSession
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bson.types.ObjectId

private const val PEEK_MISSION_SIZE = 3

class UserService(
    private val transactionService: TransactionService,
    private val userRepository: UserRepository,
    private val missionRepository: MissionRepository,
    private val pastRoutineHistoryRepository: PastRoutineHistoryRepository,
    private val missionPicker: MissionPicker
) {
    suspend fun getUserById(userId: String): User = userRepository.findById(ObjectId(userId))!!

    suspend fun findUserByUserToken(userToken: String): User? = userRepository.findByUserToken(userToken)

    suspend fun getPastRoutineHistories(userId: String): List<PastRoutineHistory> =
        pastRoutineHistoryRepository.findSortedByUserId(ObjectId(userId))

    suspend fun deleteById(userId: String): Long =
        transactionService.executeInTransaction { session ->
            userRepository.deleteById(ObjectId(userId), session)
        }

    suspend fun registerUser(
        userToken: String,
        gender: Gender,
        birthDate: BirthDate,
        birthTime: BirthTime?,
        preferredMissionCategoryList: List<MissionCategory>
    ): ObjectId? =
        transactionService.executeInTransaction { session ->
            if (userRepository.findByUserToken(userToken, session) != null) {
                throw BusinessException(ErrorCode.CONFLICT)
            }

            val user =
                User(
                    gender = gender,
                    userToken = userToken,
                    birthDate = birthDate,
                    birthTime = birthTime,
                    preferredMissionCategoryList = preferredMissionCategoryList
                )

            val updatedUser = updateUserMissions(user, session)
            val insertedId = userRepository.insertOne(updatedUser, session)
            insertedId?.asObjectId()?.value
        }

    suspend fun updateTodayMission(
        userId: String,
        missionId: String,
        finished: Boolean
    ): User =
        transactionService.executeInTransaction { session ->
            val user = userRepository.findById(ObjectId(userId), session)!!

            val mission =
                (user.todayDailyMissionList + user.longTermMission)
                    .filterNotNull()
                    .find { it.id.toString() == missionId }

            if (mission == null) {
                throw IllegalArgumentException("Mission $missionId doesn't exist")
            }

            val today = now()

            val toUpdateMission = mission.copy(finished = finished, endDate = today)

            val updated =
                user.copy(
                    longTermMission =
                        if (user.longTermMission?.id == toUpdateMission.id) {
                            toUpdateMission
                        } else {
                            user.longTermMission
                        },
                    todayDailyMissionList =
                        user.todayDailyMissionList.map {
                            if (it.id == toUpdateMission.id) {
                                toUpdateMission
                            } else {
                                it
                            }
                        }
                )

            if (userRepository.updateOne(user.id!!, updated, session) < 1L) {
                throw IllegalArgumentException("Mission $missionId fails to update.")
            }

            updated
        }

    suspend fun updateUserDailyFortune(
        userId: String,
        dailyFortune: DailyFortune
    ): User =
        transactionService.executeInTransaction { session ->
            val user = userRepository.findById(ObjectId(userId))!!
            val updatedUser = user.copy(dailyFortune = dailyFortune)
            userRepository.updateOne(user.id!!, updatedUser, session)
            updatedUser
        }

    suspend fun summaryTodayMission(
        userId: String,
        date: LocalDate
    ): Money =
        transactionService.executeInTransaction { session ->
            val user = userRepository.findById(ObjectId(userId), session)!!
            val todayMissionList = user.todayDailyMissionList

            val todaySavedMoney = todayMissionList.calculateSavedMoney()

            val pastRoutineHistory =
                PastRoutineHistory(
                    id = null,
                    userId = ObjectId(userId),
                    date = date,
                    missions = todayMissionList
                )

            if (pastRoutineHistoryRepository.findByUserIdAndDate(ObjectId(userId), date, session) != null) {
                throw BusinessException(ErrorCode.CONFLICT)
            }

            val insertedPastRoutineHistoryId = pastRoutineHistoryRepository.insertOne(pastRoutineHistory, session)!!

            val missionUpdatedUser = updateUserMissions(user, session)

            userRepository.updateOne(
                missionUpdatedUser.id!!,
                missionUpdatedUser.copy(
                    pastRoutineHistoryIds = (
                        missionUpdatedUser.pastRoutineHistoryIds + insertedPastRoutineHistoryId.asObjectId().value
                    ),
                    totalSavedMoney = user.totalSavedMoney + todaySavedMoney
                ),
                session
            )

            todaySavedMoney
        }

    private suspend fun updateUserMissions(
        user: User,
        session: ClientSession
    ): User {
        val resolvedCategory = user.resolveTodayMissionCategory()

        val dailyCategoryMissions = missionRepository.findDailyByCategory(resolvedCategory)
        val longTermCategoryMissions = missionRepository.findLongTermByCategory(resolvedCategory, session)

        val today = now()

        val updatedUser =
            user.copy(
                longTermMission =
                    if (user.longTermMission == null || user.longTermMission.finished) {
                        longTermCategoryMissions
                            .random()
                            .copy(endDate = today.plus(14, DateTimeUnit.DAY))
                    } else {
                        user.longTermMission
                    },
                todayDailyMissionList =
                    dailyCategoryMissions
                        .shuffled()
                        .take(PEEK_MISSION_SIZE)
                        .map { it.copy(endDate = today.plus(1, DateTimeUnit.DAY)) }
            )
        if (user.id != null) {
            userRepository.updateOne(user.id, updatedUser, session)
        }
        return updatedUser
    }

    suspend fun switchTodayMission(
        userId: String,
        missionId: String
    ): User =
        transactionService.executeInTransaction { session ->
            val user = userRepository.findById(ObjectId(userId), session)!!

            val reRolledTodayMissionList =
                user.todayDailyMissionList.map { todayMission ->
                    if (todayMission.id != ObjectId(missionId)) {
                        todayMission
                    } else {
                        if (todayMission.switchCount >= MAX_SWITCH_COUNT) {
                            throw BusinessException(ErrorCode.MAXIMUM_SWITCH_COUNT_EXCEEDED)
                        }

                        missionPicker
                            .pickMission(
                                existingMission = todayMission,
                                preferredMissionCategoryList = user.preferredMissionCategoryList,
                                luckTotalScore = user.dailyFortune!!.totalScore,
                                session = session
                            ).copy(switchCount = todayMission.switchCount + 1)
                    }
                }

            val longTermMission =
                user.longTermMission?.let {
                    if (it.id != ObjectId(missionId)) {
                        it
                    } else {
                        if (it.switchCount >= MAX_SWITCH_COUNT) {
                            throw BusinessException(ErrorCode.MAXIMUM_SWITCH_COUNT_EXCEEDED)
                        }

                        missionPicker
                            .pickMission(
                                existingMission = it,
                                type = MissionType.LONG_TERM,
                                preferredMissionCategoryList = user.preferredMissionCategoryList,
                                luckTotalScore = user.dailyFortune!!.totalScore,
                                session
                            ).copy(switchCount = it.switchCount + 1)
                    }
                }

            val updatedUser =
                user.copy(todayDailyMissionList = reRolledTodayMissionList, longTermMission = longTermMission)
            userRepository.updateOne(ObjectId(userId), updatedUser, session)
            updatedUser
        }

    suspend fun getTodayPastRoutines(
        userId: String,
        date: LocalDate
    ) = getPastRoutineMissionHistoriesBetween(userId, date, date)

    suspend fun getWeekPastRoutines(
        userId: String,
        date: LocalDate
    ): List<PastRoutineHistory> {
        val dayOfWeek = date.dayOfWeek

        val daysFromMonday = dayOfWeek.ordinal
        val daysToSunday = DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal

        val startOfWeek = date.minus(daysFromMonday.toLong(), DateTimeUnit.DAY)
        val endOfWeek = date.plus(daysToSunday.toLong(), DateTimeUnit.DAY)

        return getPastRoutineMissionHistoriesBetween(userId, startOfWeek, endOfWeek)
    }

    suspend fun getMonthlyPastRoutines(
        userId: String,
        date: LocalDate
    ): List<PastRoutineHistory> {
        val startOfMonth = LocalDate(date.year, date.month, 1)
        val isLeap = date.isLeapYear()
        val endOfMonth = LocalDate(date.year, date.month, date.month.length(isLeap))

        return getPastRoutineMissionHistoriesBetween(userId, startOfMonth, endOfMonth)
    }

    private suspend fun getPastRoutineMissionHistoriesBetween(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PastRoutineHistory> =
        pastRoutineHistoryRepository.findByUserIdAndDateBetween(ObjectId(userId), startDate, endDate)

    private fun User.resolveTodayMissionCategory() = this.preferredMissionCategoryList.random()

    companion object {
        const val MAX_SWITCH_COUNT = 4
    }
}

class MissionPicker(
    private val missionRepository: MissionRepository
) {
    suspend fun pickMission(
        existingMission: Mission? = null,
        type: MissionType = MissionType.DAILY,
        preferredMissionCategoryList: List<MissionCategory>,
        luckTotalScore: Int = 0,
        session: ClientSession
    ): Mission {
        val peekMissionCategory = preferredMissionCategoryList.random()
        val randomPeekMission =
            missionRepository
                .findByCategory(type, peekMissionCategory, session)
                .filter { it.id != existingMission?.id }
                .filter {
                    Difficulty.entries[it.difficulty - 1].let { difficulty: Difficulty ->
                        luckTotalScore in difficulty.minValue..difficulty.maxValue
                    }
                }.random()

        return randomPeekMission
    }
}

enum class Difficulty(
    val minValue: Int,
    val maxValue: Int
) {
    EASY(0, 33),
    MIDDLE(34, 67),
    HARD(68, 100)
}

fun now() =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

fun LocalDate.isLeapYear(): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)