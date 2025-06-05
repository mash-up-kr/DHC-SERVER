package com.mashup.com.mashup.dhc.domain.service

import com.mashup.com.mashup.dhc.domain.model.PastRoutineHistory
import com.mashup.com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.com.mashup.dhc.utils.BirthDate
import com.mashup.com.mashup.dhc.utils.Money
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.domain.service.TransactionService
import com.mashup.dhc.utils.BirthTime
import kotlinx.datetime.LocalDate
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
        birthDate: BirthDate?,
        birthTime: BirthTime?,
        preferredMissionCategoryList: List<MissionCategory>
    ) {
        userRepository.insertOne(
            User(
                gender = gender,
                userToken = userToken,
                birthDate = birthDate,
                birthTime = birthTime,
                preferredMissionCategoryList = preferredMissionCategoryList
            )
        )
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

            val resolvedCategory = user.resolveTodayMissionCategory()

            val dailyCategoryMissions = missionRepository.findDailyByCategory(resolvedCategory)
            val longTermCategoryMissions = missionRepository.findLongTermByCategory(resolvedCategory)

            val updatedUser =
                user.copy(
                    longTermMission = longTermCategoryMissions.random(),
                    todayDailyMissionList = dailyCategoryMissions.shuffled().take(PEEK_MISSION_SIZE),
                    pastRoutineHistoryIds = user.pastRoutineHistoryIds + pastRoutineHistory.id!!
                )
            userRepository.updateOne(ObjectId(userId), updatedUser)
        }
        // TODO: 트랜잭션 롤백시 이후에 복구 처리 추가 필요

        return todayMissionList
            .filter { it.finished }
            .map { it.cost }
            .reduce(Money::plus)
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