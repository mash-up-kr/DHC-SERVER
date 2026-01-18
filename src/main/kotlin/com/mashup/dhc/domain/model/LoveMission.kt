package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.Money
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

/**
 * 러브미션 템플릿 엔티티
 * - title: 미션 제목
 * - difficulty: 난이도 (1~5)
 * - cost: 예상 절약금액
 */
data class LoveMission(
    @BsonId val id: ObjectId? = null,
    val title: String,
    val difficulty: Int,
    val cost: Money
)

/**
 * 사용자의 러브미션 상태
 * - startDate: 러브미션 시작일 (최초 노출일)
 * - endDate: 러브미션 종료일 (시작일 + 13일 = 14일간)
 * - missions: 14개의 Mission 인스턴스 (각각 고유 ID, 완료 상태 보유)
 */
data class LoveMissionStatus(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val missions: List<Mission> = listOf()
) {
    companion object {
        fun create(
            startDate: LocalDate,
            missions: List<Mission>
        ): LoveMissionStatus =
            LoveMissionStatus(
                startDate = startDate,
                endDate = LocalDate.fromEpochDays(startDate.toEpochDays() + 13),
                missions = missions
            )
    }

    /**
     * 오늘이 몇 번째 날인지 계산 (1~14)
     * 기간 외면 null 반환
     */
    fun calculateDayNumber(today: LocalDate): Int? {
        val daysPassed = (today.toEpochDays() - startDate.toEpochDays()).toInt()
        return if (daysPassed in 0..13) daysPassed + 1 else null
    }

    /**
     * 해당 날짜가 유효 기간 내인지 확인
     */
    fun isActive(today: LocalDate): Boolean = today >= startDate && today <= endDate

    /**
     * 오늘의 미션 가져오기
     */
    fun getTodayMission(today: LocalDate): Mission? {
        val dayNumber = calculateDayNumber(today) ?: return null
        return missions.getOrNull(dayNumber - 1)
    }

    /**
     * missionId로 미션 찾기
     */
    fun findMissionById(missionId: String): Mission? = missions.find { it.id.toString() == missionId }

    /**
     * 남은 일수 계산
     */
    fun remainingDays(today: LocalDate): Int = (endDate.toEpochDays() - today.toEpochDays()).toInt().coerceAtLeast(0)
}

class LoveMissionRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun findAll(): List<LoveMission> =
        mongoDatabase
            .getCollection<LoveMission>(LOVE_MISSION_COLLECTION)
            .find()
            .toList()

    companion object {
        const val LOVE_MISSION_COLLECTION = "lovemission"
    }
}