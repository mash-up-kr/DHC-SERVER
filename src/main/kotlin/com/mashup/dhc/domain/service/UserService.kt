package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.PastRoutineHistory
import com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.utils.BirthTime
import com.mashup.dhc.utils.Money
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bson.BsonValue
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

    suspend fun getPastRoutineHistories(userId: String): List<PastRoutineHistory> =
        pastRoutineHistoryRepository.findSortedByUserId(ObjectId(userId))

    suspend fun registerUser(
        userToken: String,
        gender: Gender,
        birthDate: BirthDate,
        birthTime: BirthTime?,
        preferredMissionCategoryList: List<MissionCategory>
    ): BsonValue? {
        val user = User(
            gender = gender,
            userToken = userToken,
            birthDate = birthDate,
            birthTime = birthTime,
            preferredMissionCategoryList = preferredMissionCategoryList
        )

        return userRepository.insertOne(updateUserMissions(user))
    }

    suspend fun updateTodayMission(
        userId: String,
        missionId: String,
        finished: Boolean
    ): User {
        val user = getUserById(userId)

        val mission =
            (user.todayDailyMissionList + user.longTermMission)
                .filterNotNull()
                .find { it.id.toString() == missionId }

        if (mission == null) {
            throw IllegalArgumentException("Mission $missionId doesn't exist")
        }

        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

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

        if (userRepository.updateOne(user.id!!, updated) < 1L) {
            throw IllegalArgumentException("Mission $missionId fails to update.")
        }

        return updated
    }

    suspend fun summaryTodayMission(
        userId: String,
        date: LocalDate
    ): Money {
        val user = userRepository.findById(ObjectId(userId))!!
        val todayMissionList = user.todayDailyMissionList

        transactionService.executeInTransaction {
            val pastRoutineHistory =
                PastRoutineHistory(
                    id = null,
                    userId = ObjectId(userId),
                    date = date,
                    missions = todayMissionList
                )

            pastRoutineHistoryRepository.insertOne(pastRoutineHistory)

            val missionUpdatedUser = updateUserMissions(user)

            userRepository.updateOne(
                missionUpdatedUser.id!!,
                missionUpdatedUser.copy(
                    pastRoutineHistoryIds = (
                        missionUpdatedUser.pastRoutineHistoryIds +
                            pastRoutineHistory.id!!
                    )
                )
            )
        }
        // TODO: 트랜잭션 롤백시 이후에 복구 처리 추가 필요

        return todayMissionList
            .filter { it.finished }
            .map { it.cost }
            .reduce(Money::plus)
    }

    private suspend fun updateUserMissions(user: User): User {
        val resolvedCategory = user.resolveTodayMissionCategory()

        val dailyCategoryMissions = missionRepository.findDailyByCategory(resolvedCategory)
        val longTermCategoryMissions = missionRepository.findLongTermByCategory(resolvedCategory)

        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

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
        userRepository.updateOne(user.id!!, updatedUser)
        return updatedUser
    }

    suspend fun switchTodayMission(
        userId: String,
        missionId: String
    ): Long {
        val user = userRepository.findById(ObjectId(userId))!!

        val reRolledTodayMissionList =
            user.todayDailyMissionList.map { todayMission ->
                if (todayMission.id == ObjectId(missionId)) {
                    missionPicker.pickMission(user.preferredMissionCategoryList)
                } else {
                    todayMission
                }
            }

        val updatedUser = user.copy(todayDailyMissionList = reRolledTodayMissionList)
        return userRepository.updateOne(ObjectId(userId), updatedUser)
    }

    suspend fun getPasRoutineMissionHistoriesWhen(
        userId: String,
        date: LocalDate
    ): PastRoutineHistory = pastRoutineHistoryRepository.findByUserIdAndDate(ObjectId(userId), date)!!

    private fun User.resolveTodayMissionCategory() = this.preferredMissionCategoryList.random()
}

class MissionPicker(
    private val missionRepository: MissionRepository
) {
    suspend fun pickMission(preferredMissionCategoryList: List<MissionCategory>): Mission {
        val peekMissionCategory = preferredMissionCategoryList.random()
        val randomPeekMission =
            missionRepository.findDailyByCategory(peekMissionCategory).random()

        return randomPeekMission
    }
}