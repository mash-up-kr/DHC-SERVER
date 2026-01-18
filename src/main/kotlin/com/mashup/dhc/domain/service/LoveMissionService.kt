package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.LoveMissionRepository
import com.mashup.dhc.domain.model.LoveMissionStatus
import com.mashup.dhc.domain.model.Mission
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.ShareRepository
import com.mashup.dhc.domain.model.User
import com.mashup.dhc.domain.model.UserRepository
import kotlinx.datetime.LocalDate
import org.bson.types.ObjectId

data class LoveMissionInfo(
    val missionId: String,
    val dayNumber: Int,
    val title: String,
    val finished: Boolean,
    val remainingDays: Int
)

class LoveMissionService(
    private val userRepository: UserRepository,
    private val shareRepository: ShareRepository,
    private val loveMissionRepository: LoveMissionRepository
) {
    /**
     * Home API 호출 시 러브미션을 확인하고 필요시 활성화합니다.
     *
     * 로직:
     * 1. 사용자에게 이미 러브미션이 있으면 → 오늘의 러브미션 반환
     * 2. 러브미션이 없고, 완료된 공유가 있으면 → 러브미션 활성화 후 반환
     * 3. 둘 다 아니면 → null 반환
     */
    suspend fun checkAndGetTodayLoveMission(
        user: User,
        today: LocalDate
    ): LoveMissionInfo? {
        val userId = user.id ?: return null

        // 이미 러브미션이 있는 경우
        val existingStatus = user.loveMissionStatus
        if (existingStatus != null) {
            return getTodayLoveMissionFromStatus(existingStatus, today)
        }

        // 러브미션이 없는 경우: 완료된 공유가 있는지 확인
        val hasCompletedShare = shareRepository.hasCompletedShare(userId)
        if (!hasCompletedShare) {
            return null
        }

        // 완료된 공유가 있으면 러브미션 활성화
        val newStatus = activateLoveMission(today)
        userRepository.updateLoveMissionStatus(userId, newStatus)

        return getTodayLoveMissionFromStatus(newStatus, today)
    }

    /**
     * 러브미션을 활성화합니다.
     * 14개의 LoveMission 템플릿을 랜덤하게 셔플하여 Mission 인스턴스를 생성합니다.
     * 각 사용자마다 다른 순서로 미션이 배정됩니다.
     */
    private suspend fun activateLoveMission(startDate: LocalDate): LoveMissionStatus {
        val loveMissionTemplates = loveMissionRepository.findAll().shuffled()

        val missions =
            loveMissionTemplates.mapIndexed { index, template ->
                val dayNumber = index + 1
                Mission(
                    id = ObjectId(),
                    category = MissionCategory.SOCIAL, // 러브미션은 사교 카테고리로 분류
                    difficulty = template.difficulty,
                    type = MissionType.LOVE,
                    finished = false,
                    title = template.title,
                    cost = template.cost,
                    switchCount = 0,
                    endDate = LocalDate.fromEpochDays(startDate.toEpochDays() + dayNumber - 1)
                )
            }

        return LoveMissionStatus.create(startDate, missions)
    }

    /**
     * 러브미션 상태에서 오늘의 러브미션 정보를 가져옵니다.
     */
    private fun getTodayLoveMissionFromStatus(
        status: LoveMissionStatus,
        today: LocalDate
    ): LoveMissionInfo? {
        // 기간 체크
        if (!status.isActive(today)) {
            return null
        }

        // 오늘이 몇 번째 날인지 계산
        val dayNumber = status.calculateDayNumber(today) ?: return null

        // 오늘의 미션 가져오기
        val todayMission = status.getTodayMission(today) ?: return null

        return LoveMissionInfo(
            missionId = todayMission.id.toString(),
            dayNumber = dayNumber,
            title = todayMission.title,
            finished = todayMission.finished,
            remainingDays = status.remainingDays(today)
        )
    }

    /**
     * 러브미션을 완료 처리합니다.
     * 기존 미션 API와 동일한 방식으로 Mission의 finished 상태를 업데이트합니다.
     *
     * @return 업데이트된 User 또는 null (실패 시)
     */
    suspend fun updateLoveMissionFinished(
        userId: ObjectId,
        missionId: String,
        finished: Boolean
    ): User? {
        val user = userRepository.findById(userId) ?: return null
        val status = user.loveMissionStatus ?: return null

        // 미션 찾기
        val targetMission = status.findMissionById(missionId) ?: return null

        // 업데이트된 미션 리스트 생성
        val updatedMissions =
            status.missions.map { mission ->
                if (mission.id.toString() == missionId) {
                    mission.copy(finished = finished)
                } else {
                    mission
                }
            }

        val updatedStatus = status.copy(missions = updatedMissions)
        userRepository.updateLoveMissionStatus(userId, updatedStatus)

        return userRepository.findById(userId)
    }
}