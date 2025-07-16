package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneRepository
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory

class FortuneService(
    private val backgroundScope: CoroutineScope,
    private val userService: UserService,
    private val fortuneRepository: FortuneRepository,
    private val geminiService: GeminiService,
    private val mutexManager: MutexManager,
    private val dailyBatchQueue: ConcurrentLinkedQueue<Pair<String, GeminiFortuneRequest>>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startDailyBatch() {
        backgroundScope.launch {
            logger.info("Daily batch processor started")

            while (isActive) { // coroutine이 취소되면 종료
                try {
                    delay(1.minutes)

                    val requests = mutableListOf<Pair<String, GeminiFortuneRequest>>()
                    val batchSize = 15

                    repeat(batchSize) {
                        // poll()은 큐가 비어있으면 null 반환
                        val request = dailyBatchQueue.poll() ?: return@repeat
                        requests.add(request)
                    }

                    // 빈 리스트인 경우 API 호출하지 않음
                    if (requests.isNotEmpty()) {
                        logger.info("Processing batch of ${requests.size} fortune requests")

                        try {
                            geminiService.generateDailyFortuneBatch(requests)
                            logger.info("Successfully processed ${requests.size} fortune requests")
                        } catch (e: Exception) {
                            logger.error("Failed to process batch fortune requests", e)

                            requests.forEach { dailyBatchQueue.offer(it) }

                            delay(2.minutes)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Unexpected error in daily batch processor", e)
                    delay(2.minutes)
                }
            }

            logger.info("Daily batch processor stopped")
        }
    }

    fun enqueueGenerateDailyFortuneTask(
        userId: String,
        geminiFortuneRequest: GeminiFortuneRequest
    ) {
        dailyBatchQueue.add(userId to geminiFortuneRequest)
    }

    suspend fun enqueueGenerateFortuneTask(
        userId: String,
        requestDate: LocalDate
    ) {
        val lockKey = "$userId-${requestDate.year}-${requestDate.monthNumber}"

        mutexManager.withLock(lockKey) {
            val user = userService.getUserById(userId)
            if (user.dailyFortunes?.findDailyFortune(requestDate) != null) {
                throw RuntimeException("Fortune Cache already exists")
            }

            backgroundScope.launch {
                try {
                    geminiService.generateMonthlyFortuneWithBatch(
                        userId = userId,
                        request = user.toGeminiFortuneRequest()
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

        if (user.dailyFortune == null
            || user.dailyFortune.date != requestDate.toYearMonthDayString()
        ) {
            if (user.dailyFortunes?.findDailyFortune(requestDate) != null) {
                return user.dailyFortunes.findDailyFortune(requestDate)!!
            }

            val dailyFortune = fortuneRepository.retrieveArbitraryDailyFortune()!!
            user = userService.updateUserDailyFortune(
                user.id!!.toHexString(),
                dailyFortune.copy(date = requestDate.toYearMonthDayString())
            )
            return user.dailyFortune!!
        }

        return user.dailyFortune
    }
}

private fun List<DailyFortune>.findDailyFortune(targetDate: LocalDate): DailyFortune? {
    val targetDateStr = targetDate.toYearMonthDayString()
    return this.find { it.date == targetDateStr }
}

private fun LocalDate.toYearMonthDayString() =
    "${this.year}-${
        this.month.value.toString().padStart(
            2,
            '0'
        )
    }-${this.dayOfMonth.toString().padStart(2, '0')}"