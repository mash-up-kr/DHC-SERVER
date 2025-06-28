package com.mashup.dhc.domain.task

import com.mashup.dhc.utils.LockableTask
import com.mashup.dhc.utils.UserTask

/**
 * 운세 생성 작업
 */
data class FortuneGenerationTask(
    override val userId: String,
    val year: Int,
    val month: Int,
    override val lockKey: String
) : UserTask, LockableTask {
    override val taskId: String = "$userId-$year-$month"
    override val taskType: String = "FortuneGeneration"

    /**
     * 작업 설명 (로깅용)
     */
    fun getDescription(): String = "운세 생성 작업 - 사용자: $userId, 연월: $year-$month"

    /**
     * 작업 키 생성 (중복 작업 체크용)
     */
    fun getWorkKey(): String = "$userId-$year-$month"
}