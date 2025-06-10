package com.mashup.dhc.domain.service

import com.mashup.com.mashup.dhc.domain.service.LockRegistry
import com.mashup.com.mashup.dhc.domain.service.UserService
import com.mashup.com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.FortuneCache
import com.mashup.dhc.domain.model.FortuneRequest
import com.mashup.dhc.domain.model.FortuneResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FortuneService(
    private val backgroundScope: CoroutineScope,
    private val userService: UserService,
    private val fortuneRepository: FortuneRepository,
    private val geminiService: GeminiService,
    private val headerBodyService: FortuneHeaderBodyService
) {
    private val log = LoggerFactory.getLogger(FortuneService::class.java)

    suspend fun executeFortuneGenerationTask(
        userId: String,
        year: Int,
        month: Int
    ) {
        val user = userService.getUserById(userId)
        fortuneRepository.saveFortune(
            geminiService.generateFortune(
                FortuneRequest(
                    user.gender.toString(),
                    user.birthDate.toString(),
                    user.birthTime?.toString()
                        ?: throw RuntimeException(), //TODO 커스텀 예외 붙이기
                    year,
                    month,
                )
            ).toFortuneCache(userId)
        )

    }

    suspend fun addFortuneGenerationTask(userId: String, requestDate: LocalDate) {

        log.info("requestDate: $requestDate, userId: $userId")

        val lockKey = "${userId}-${requestDate.year}-${requestDate.monthValue}"
        log.info("Lock Key: $lockKey")
        if (!LockRegistry.tryLock(lockKey)) throw RuntimeException("Lock key is using") //TODO 커스텀 예외 붙이기
        // DB에 이미 금전운이 존재하는지 확인
        if (checkIfFortuneCached(userId, requestDate)) {
            LockRegistry.unlock(lockKey)
            throw RuntimeException("Fortune Cache already exists") //TODO 커스텀 예외 붙이기
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

    suspend fun checkIfFortuneCached(userId: String, requestDate: LocalDate): Boolean {
        return fortuneRepository.getFortuneByMonth(userId, requestDate.year, requestDate.monthValue) != null
    }

    suspend fun queryDailyFortune(userId: String, requestDate: LocalDate): DailyFortune {
        val user = userService.getUserById(userId)
        return fortuneRepository.getFortuneByMonth(userId, requestDate.year, requestDate.monthValue)
            ?.toDailyFortune(requestDate, user.birthDate)
            ?: throw RuntimeException("Fortune Cache not found")
    }

    /**
     * 캐시된 데이터를 클라이언트 응답으로 변환 후 특정 날짜 운세 추출
     */
    private fun FortuneCache.toDailyFortune(requestDate: LocalDate, birthDate: BirthDate): DailyFortune =
        headerBodyService.convertToClientResponse(fortune, birthDate, month, year).findDailyFortune(requestDate)
            ?: throw RuntimeException("DailyFortune not found")

}


private fun FortuneResult.findDailyFortune(targetDate: LocalDate): DailyFortune? {
    val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return fortune.find { it.date == targetDateStr }
}


