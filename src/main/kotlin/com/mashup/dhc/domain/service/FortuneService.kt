package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.MonthlyFortune
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
        log.info("requestDate: $requestDate, userId: $userId")

        val lockKey = "$userId-${requestDate.year}-${requestDate.monthValue}"
        log.info("Lock Key: $lockKey")
        if (!LockRegistry.tryLock(lockKey)) throw RuntimeException("Lock key is using") // TODO 커스텀 예외 붙이기
        // DB에 이미 금전운이 존재하는지 확인
        if (checkIfFortuneCached(userId, requestDate)) {
            LockRegistry.unlock(lockKey)
            throw RuntimeException("Fortune Cache already exists") // TODO 커스텀 예외 붙이기
        }

        backgroundScope.launch {
            LockRegistry.finallyUnlock(lockKey) {
                executeFortuneGenerationTask(
                    userId,
                    requestDate.year,
                    requestDate.monthValue
                )
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
}

private fun MonthlyFortune.findDailyFortune(targetDate: LocalDate): DailyFortune? {
    val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return dailyFortuneList.find { it.date == targetDateStr }
}