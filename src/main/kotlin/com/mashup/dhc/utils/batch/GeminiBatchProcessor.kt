package com.mashup.dhc.utils.batch

import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.MonthlyFortune
import com.mashup.dhc.domain.service.GeminiBatchFortuneResponse
import com.mashup.dhc.domain.service.GeminiFortuneRequest
import com.mashup.dhc.domain.service.GeminiFortuneResponse
import com.mashup.dhc.domain.service.GeminiService
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory

class GeminiBatchProcessor(
    private val geminiService: GeminiService,
    private val batchIntervalMs: Long = 4000L
) {
    private val pendingRequests = ConcurrentLinkedQueue<GeminiBatchRequest>()
    private val processingMutex = Mutex()
    private val logger = LoggerFactory.getLogger(this::class.java)

    sealed class GeminiBatchRequest(
        val timestamp: Long = System.currentTimeMillis()
    )

    class MonthBatchRequest(
        val userId: String,
        val request: GeminiFortuneRequest,
        val continuation: Continuation<GeminiFortuneResponse>,
        timestamp: Long = System.currentTimeMillis(),
        val monthlyFortuneHandler: suspend (MonthlyFortune) -> Unit
    ) : GeminiBatchRequest(timestamp)

    class DailyBatchRequest(
        val requests: List<Pair<String, GeminiFortuneRequest>>,
        val continuation: Continuation<GeminiBatchFortuneResponse>,
        timestamp: Long = System.currentTimeMillis(),
        val dailyFortunesHandler: suspend (Map<String, List<DailyFortune>>) -> Unit
    ) : GeminiBatchRequest(timestamp)

    // 서버 시작 시 호출하여 배치 프로세서를 상시 실행
    fun startBatchProcessor(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                delay(batchIntervalMs)

                if (pendingRequests.isNotEmpty()) {
                    processingMutex.withLock {
                        processBatch()
                    }
                }
            }
        }
    }

    suspend fun generateMonthlyFortune(
        userId: String,
        request: GeminiFortuneRequest,
        monthlyFortuneHandler: suspend (MonthlyFortune) -> Unit
    ): GeminiFortuneResponse =
        suspendCoroutine { continuation ->
            val monthBatchRequest =
                MonthBatchRequest(userId, request, continuation, monthlyFortuneHandler = monthlyFortuneHandler)
            pendingRequests.offer(monthBatchRequest)
        }

    suspend fun generateDailyFortune(
        request: List<Pair<String, GeminiFortuneRequest>>,
        dailyFortunesHandler: suspend (Map<String, List<DailyFortune>>) -> Unit
    ): GeminiBatchFortuneResponse =
        suspendCoroutine { continuation ->
            val dailyBatchRequest =
                DailyBatchRequest(request, continuation, dailyFortunesHandler = dailyFortunesHandler)
            pendingRequests.offer(dailyBatchRequest)
        }

    private suspend fun processBatch(startInstant: Instant = Clock.System.now()) {
        val timeoutInstant = startInstant.plus(1.minutes)

        var processedCount = 0
        logger.info("배치 진행중.. 총 ${pendingRequests.size} 개 요청 처리중, 현재 시각 $startInstant, 타임아웃 시간 $timeoutInstant")
        while (pendingRequests.isNotEmpty()) {
            val requestInstant = Clock.System.now()
            if (requestInstant > timeoutInstant || processedCount > MAX_PROCESSED_REQUEST_PER_BATCH) {
                break
            }

            val request = pendingRequests.poll()

            try {
                val processingTimeMillis =
                    measureTimeMillis {
                        when (request) {
                            is MonthBatchRequest -> {
                                logger.info("한 달 배치 처리 시작: userId=${request.userId}")
                                val response = geminiService.requestMonthFortune(request.request)
                                val monthlyFortune = response.toMonthlyFortune()

                                request.monthlyFortuneHandler(monthlyFortune)
                                request.continuation.resume(response)
                            }

                            is DailyBatchRequest -> {
                                logger.info("하루치 배치 처리 시작")
                                val response = geminiService.requestDailyBatchFortune(request.requests)
                                logger.info("응답 $response")
                                val dailyFortunesByUserId = response.results.associate { it.userId to it.fortune }

                                request.dailyFortunesHandler(dailyFortunesByUserId)
                                request.continuation.resume(response)
                            }
                        }
                    }

                processedCount += 1
                logger.info("{} 건 처리 중 진행시간 {}", processedCount, processingTimeMillis / 1000)
            } catch (e: Exception) {
                logger.error("배치 처리 중 실패. 요청: {}", request, e)
            }
        }
    }

    companion object {
        const val MAX_PROCESSED_REQUEST_PER_BATCH = 15
    }
}