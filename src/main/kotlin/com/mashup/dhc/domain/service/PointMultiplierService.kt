package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.PastRoutineHistory
import kotlinx.datetime.LocalDate

/**
 * 포인트 배율 정책 인터페이스
 */
interface PointMultiplierPolicy {
    fun isApplicable(
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>,
        latestHistoryDate: LocalDate?
    ): Boolean

    fun getMultiplier(): Int
}

/**
 * 어제 미션 전부 미수행 시 2배 배율 정책
 * - 어제 기록이 있지만 모든 미션 중 성공한 게 없는 경우
 * - 장기부재(2일+)가 아닌 경우에만 적용
 */
class YesterdayMissionFailedPolicy : PointMultiplierPolicy {
    override fun isApplicable(
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>,
        latestHistoryDate: LocalDate?
    ): Boolean {
        // 장기부재인 경우 이 정책은 적용하지 않음 (LongAbsencePolicy가 적용됨)
        if (latestHistoryDate != null) {
            val daysSinceLastHistory = (today.toEpochDays() - latestHistoryDate.toEpochDays()).toInt()
            if (daysSinceLastHistory >= 2) {
                return false
            }
        }

        // 어제 기록이 없으면 적용하지 않음
        if (yesterdayHistory.isEmpty()) {
            return false
        }

        // 어제 모든 미션 중 성공한 게 없으면 적용
        val yesterdayMissions = yesterdayHistory.flatMap { it.missions }
        return yesterdayMissions.isNotEmpty() && yesterdayMissions.none { it.finished }
    }

    override fun getMultiplier(): Int = 2
}

/**
 * 장기부재(2일 이상 미정산) 시 4배 배율 정책
 * - 가장 최근 pastRoutineHistory 기준 2일 이상 경과
 */
class LongAbsencePolicy : PointMultiplierPolicy {
    override fun isApplicable(
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>,
        latestHistoryDate: LocalDate?
    ): Boolean {
        val lastDate = latestHistoryDate ?: return false
        val daysSinceLastHistory = (today.toEpochDays() - lastDate.toEpochDays()).toInt()
        return daysSinceLastHistory >= 2
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
    fun calculateMultiplier(
        today: LocalDate,
        yesterdayHistory: List<PastRoutineHistory>,
        latestHistoryDate: LocalDate?
    ): Int =
        policies
            .filter { it.isApplicable(today, yesterdayHistory, latestHistoryDate) }
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