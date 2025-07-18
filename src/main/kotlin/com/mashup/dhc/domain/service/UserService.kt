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

        val today: LocalDate = if (!user.todayDailyMissionList.isEmpty()) {
            user.todayDailyMissionList.first().endDate
        } else {
            null
        } ?: now()

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
                                luckTotalScore = user.dailyFortune?.totalScore ?: 50, // null일 경우 기본값 50
                                session = session
                            ).copy(
                                endDate = now().plus(1, DateTimeUnit.DAY),
                                switchCount = todayMission.switchCount + 1
                            )
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
                                luckTotalScore = user.dailyFortune?.totalScore ?: 50, // null일 경우 기본값 50
                                session
                            ).copy(endDate = now().plus(14, DateTimeUnit.DAY), switchCount = it.switchCount + 1)
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

    suspend fun addJulyPastRoutineHistories(
        userId: String,
        year: Int,
        julyData: Map<Int, Int>
    ): List<PastRoutineHistory> =
        transactionService.executeInTransaction { session ->
            val user =
                userRepository.findById(ObjectId(userId), session)
                    ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

            val addedHistories = mutableListOf<PastRoutineHistory>()
            val allMissions = missionRepository.findAll()

            // 카테고리별 미션 그룹화
            val missionsByCategory =
                allMissions
                    .filter { it.type == MissionType.DAILY }
                    .groupBy { it.category }

            // 사용자의 선호 카테고리에서 미션 선택
            val preferredCategories = user.preferredMissionCategoryList

            val now = now()
            julyData.forEach { (day, completedCount) ->
                val date = LocalDate(year, 7, day)

                if (date >= now) {
                    return@forEach
                }

                // 이미 존재하는지 확인
                val existing = pastRoutineHistoryRepository.findByUserIdAndDate(ObjectId(userId), date, session)
                if (existing != null) {
                    return@forEach
                }

                // 3개의 미션을 랜덤하게 선택
                val selectedMissions = mutableListOf<Mission>()

                // 각 선호 카테고리에서 미션을 선택
                preferredCategories.take(3).forEach { category ->
                    val categoryMissions = missionsByCategory[category] ?: emptyList()
                    if (categoryMissions.isNotEmpty()) {
                        selectedMissions.add(categoryMissions.random())
                    }
                }

                // 선호 카테고리가 3개 미만인 경우 랜덤하게 추가
                while (selectedMissions.size < 3) {
                    val randomCategory =
                        MissionCategory.entries
                            .filter { it != MissionCategory.SELF_REFLECTION }
                            .random()
                    val categoryMissions = missionsByCategory[randomCategory] ?: emptyList()
                    if (categoryMissions.isNotEmpty()) {
                        selectedMissions.add(categoryMissions.random())
                    }
                }

                // 완료된 미션 설정
                val missionsWithStatus =
                    selectedMissions.take(3).mapIndexed { index, mission ->
                        mission.copy(
                            id = ObjectId(),
                            finished = index < completedCount,
                            endDate = date
                        )
                    }

                val pastRoutineHistory =
                    PastRoutineHistory(
                        id = null,
                        userId = ObjectId(userId),
                        date = date,
                        missions = missionsWithStatus
                    )

                val insertedId = pastRoutineHistoryRepository.insertOne(pastRoutineHistory, session)
                if (insertedId != null) {
                    addedHistories.add(pastRoutineHistory.copy(id = insertedId.asObjectId().value))

                    // user의 pastRoutineHistoryIds 업데이트
                    val updatedUser =
                        user.copy(
                            pastRoutineHistoryIds = user.pastRoutineHistoryIds + insertedId.asObjectId().value,
                            totalSavedMoney = user.totalSavedMoney + missionsWithStatus.calculateSavedMoney()
                        )
                    userRepository.updateOne(user.id!!, updatedUser, session)
                }
            }

            addedHistories
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
        val availableMissions =
            missionRepository
                .findByCategory(type, peekMissionCategory, session)
                .filter { it.id != existingMission?.id }
                .filter {
                    Difficulty.entries[it.difficulty - 1].let { difficulty: Difficulty ->
                        luckTotalScore in difficulty.minValue..difficulty.maxValue
                    }
                }

        if (availableMissions.isEmpty()) {
            // 필터링된 미션이 없으면 기존 미션 제외하고 다시 검색
            val fallbackMissions =
                missionRepository
                    .findByCategory(type, peekMissionCategory, session)
                    .filter { it.id != existingMission?.id }

            if (fallbackMissions.isEmpty()) {
                throw IllegalStateException("No missions available for category $peekMissionCategory and type $type")
            }

            return fallbackMissions.random()
        }

        return availableMissions.random()
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