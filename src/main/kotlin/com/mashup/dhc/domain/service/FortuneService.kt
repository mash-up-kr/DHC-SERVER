package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.routes.BusinessException
import com.mashup.dhc.routes.ErrorCode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FortuneService(
    private val backgroundScope: CoroutineScope,
    private val userService: UserService,
    private val fortuneRepository: FortuneRepository,
    private val geminiService: GeminiService,
    private val mutexManager: MutexManager
) {
    suspend fun enqueueGenerateFortuneTask(
        userId: String,
        requestDate: LocalDate
    ) {
        val lockKey = "$userId-${requestDate.year}-${requestDate.monthValue}"

        mutexManager.withLock(lockKey) {
            val user = userService.getUserById(userId)
            if (user.monthlyFortune?.findDailyFortune(requestDate) != null) {
                throw RuntimeException("Fortune Cache already exists")
            }

            backgroundScope.launch {
                try {
                    geminiService.generateFortuneWithBatch(
                        userId = userId,
                        request =
                            GeminiFortuneRequest(
                                user.gender.toString(),
                                user.birthDate.toString(),
                                user.birthTime?.toString()
                                    ?: throw RuntimeException("birthTime이 null입니다. userId: $userId"),
                                requestDate.year,
                                requestDate.monthValue
                            )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }

    suspend fun queryDailyFortune(
        userId: String,
        requestDate: LocalDate
    ): DailyFortune {
        var user = userService.getUserById(userId)
        if (user.dailyFortune?.date != requestDate.toYearMonthDayString()) {
            val dailyFortune = fortuneRepository.retrieveArbitraryDailyFortune()
            if (dailyFortune != null) {
                user = userService.updateUserDailyFortune(user.id!!.toHexString(), dailyFortune)
            }
        }

        return user.dailyFortune?.copy(date = requestDate.toYearMonthDayString())
            ?: throw BusinessException(ErrorCode.NOT_FOUND)
    }
}

private fun MonthlyFortune.findDailyFortune(targetDate: LocalDate): DailyFortune? {
    val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return dailyFortuneList.find { it.date == targetDateStr }
}

private fun LocalDate.toYearMonthDayString() =
    "${this.year}-${
        this.month.value.toString().padStart(
            2,
            '0'
        )
    }-${this.dayOfMonth.toString().padStart(2, '0')}"