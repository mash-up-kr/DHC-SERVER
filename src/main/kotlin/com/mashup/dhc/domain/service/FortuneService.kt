package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.task.FortuneGenerationTask
import com.mashup.dhc.utils.BatchProcessor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class FortuneService(
    private val backgroundScope: CoroutineScope,
    private val userService: UserService,
    private val fortuneRepository: FortuneRepository,
    private val geminiService: GeminiService
) {
    private val log = LoggerFactory.getLogger(FortuneService::class.java)

    private val batchProcessor =
        BatchProcessor(
            backgroundScope = backgroundScope,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = ::executeBatch
        )

    private suspend fun executeFortuneGenerationTask(
        userId: String,
        year: Int,
        month: Int
    ) {
        val user = userService.getUserById(userId)
        fortuneRepository.upsertMonthlyFortune(
            userId,
            geminiService
                .generateFortune(
                    GeminiFortuneRequest(
                        user.gender.toString(),
                        user.birthDate.toString(),
                        user.birthTime?.toString()
                            ?: throw RuntimeException(), // TODO 커스텀 예외 붙이기
                        year,
                        month
                    )
                ).toMonthlyFortune()
        )
    }

    suspend fun addFortuneGenerationTask(
        userId: String,
        requestDate: LocalDate
    ) {
        val lockKey = "$userId-${requestDate.year}-${requestDate.monthValue}"
        if (!LockRegistry.tryLock(lockKey)) throw RuntimeException("Lock key is using") // TODO 커스텀 예외 붙이기

        // DB에 이미 운세가 존재하는지 확인
        if (checkIfFortuneCached(userId, requestDate)) {
            LockRegistry.unlock(lockKey)
            throw RuntimeException("Fortune Cache already exists") // TODO 커스텀 예외 붙이기
        }

        try {
            // 배치 처리기에 작업 추가
            val task =
                FortuneGenerationTask(
                    userId = userId,
                    year = requestDate.year,
                    month = requestDate.monthValue,
                    lockKey = lockKey
                )

            batchProcessor.addTask(task)
        } catch (e: Exception) {
            LockRegistry.unlock(lockKey)
            throw e
        }
    }

    /**
     * 배치 내의 각 작업을 순차적으로 실행
     * BatchProcessor의 batchExecutor로 사용됨
     */
    private fun executeBatch(batch: List<FortuneGenerationTask>) {
        batch.forEach { task ->
            backgroundScope.launch {
                LockRegistry.finallyUnlock(task.lockKey) {
                    try {
                        log.debug("${task.getDescription()} 처리 시작")
                        executeFortuneGenerationTask(task.userId, task.year, task.month)
                        log.debug("${task.getDescription()} 처리 완료")
                    } catch (e: Exception) {
                        log.error("${task.getDescription()} 처리 실패", e)
                        throw e
                    }
                }
            }
        }
    }

    private suspend fun checkIfFortuneCached(
        userId: String,
        requestDate: LocalDate
    ): Boolean = userService.getUserById(userId).monthlyFortune?.findDailyFortune(requestDate) != null

    suspend fun queryDailyFortune(
        userId: String,
        requestDate: LocalDate
    ): DailyFortune {
        val user = userService.getUserById(userId)
        return user.monthlyFortune?.findDailyFortune(requestDate)
            ?: throw RuntimeException("Unable to find daily fortune") // TODO 커스텀 예외 붙이기
    }

    /**
     * 현재 대기 중인 요청 수 반환
     */
    fun getPendingRequestCount(): Int = batchProcessor.getPendingCount()

}

private fun MonthlyFortune.findDailyFortune(targetDate: LocalDate): DailyFortune? {
    val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return dailyFortuneList.find { it.date == targetDateStr }
}