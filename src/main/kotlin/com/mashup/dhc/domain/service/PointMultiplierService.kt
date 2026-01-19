package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.MissionType
import com.mashup.dhc.domain.model.PastRoutineHistory
import com.mashup.dhc.domain.model.User
import kotlinx.datetime.LocalDate

/**
 * 포인트 배율 정책 인터페이스
 * 새로운 배율 정책을 추가하려면 이 인터페이스를 구현하면 됩니다.
 */
interface PointMultiplierPolicy {
    /**
     * 이 정책이 적용 가능한지 확인합니다.
     */
    fun isApplicable(
        user: User,
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>
    ): Boolean

    /**
     * 적용할 배율을 반환합니다.
     */
    fun getMultiplier(): Int
}

/**
 * 어제 미션 실패 시 2배 배율 정책
 * - 어제 기록이 있지만 DAILY 미션 중 성공한 게 없는 경우
 * - 장기부재(2일+)가 아닌 경우에만 적용
 */
class YesterdayMissionFailedPolicy : PointMultiplierPolicy {
    override fun isApplicable(
        user: User,
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>
    ): Boolean {
        // 장기부재인 경우 이 정책은 적용하지 않음 (LongAbsencePolicy가 적용됨)
        val lastAccess = user.lastAccessDate
        if (lastAccess != null) {
            val daysSinceLastAccess = (today.toEpochDays() - lastAccess.toEpochDays()).toInt()
            if (daysSinceLastAccess >= 2) {
                return false
            }
        }

        // 어제 기록이 없으면 적용하지 않음
        if (yesterdayHistory.isEmpty()) {
            return false
        }

        // 어제 DAILY 미션 중 성공한 게 없으면 적용
        val yesterdayDailyMissions =
            yesterdayHistory
                .flatMap { it.missions }
                .filter { it.type == MissionType.DAILY }

        return yesterdayDailyMissions.isNotEmpty() && yesterdayDailyMissions.none { it.finished }
    }

    override fun getMultiplier(): Int = 2
}

/**
 * 장기부재(2일 이상 미접속) 시 4배 배율 정책
 */
class LongAbsencePolicy : PointMultiplierPolicy {
    override fun isApplicable(
        user: User,
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>
    ): Boolean {
        val lastAccess = user.lastAccessDate ?: return false
        val daysSinceLastAccess = (today.toEpochDays() - lastAccess.toEpochDays()).toInt()
        return daysSinceLastAccess >= 2
    }

    override fun getMultiplier(): Int = 4
}

/**
 * 포인트 배율 계산 서비스
 * 여러 정책 중 적용 가능한 정책의 최대 배율을 반환합니다.
 */
class PointMultiplierService(
    private val policies: List<PointMultiplierPolicy> = defaultPolicies()
) {
    /**
     * 현재 상황에 적용 가능한 최대 배율을 계산합니다.
     *
     * @param user 사용자 정보
     * @param today 오늘 날짜
     * @param yesterdayHistory 어제의 미션 기록
     * @return 적용할 배율 (기본값 1)
     */
    fun calculateMultiplier(
        user: User,
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>
    ): Int =
        policies
            .filter { it.isApplicable(user, today, yesterdayHistory) }
            .maxOfOrNull { it.getMultiplier() }
            ?: 1

    companion object {
        fun defaultPolicies(): List<PointMultiplierPolicy> =
            listOf(
                YesterdayMissionFailedPolicy(),
                LongAbsencePolicy()
            )
    }
}