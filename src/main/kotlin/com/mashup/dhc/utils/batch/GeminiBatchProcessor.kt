package com.mashup.dhc.utils.batch

import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.service.GeminiFortuneRequest
import com.mashup.dhc.domain.service.GeminiFortuneResponse
import com.mashup.dhc.domain.service.GeminiService
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

class GeminiBatchProcessor(
    private val geminiService: GeminiService,
    private val batchIntervalMs: Long = 4000L
) {
    private val pendingRequests = ConcurrentLinkedQueue<BatchRequest>()
    private val processingMutex = Mutex()
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class BatchRequest(
        val userId: String,
        val request: GeminiFortuneRequest,
        val continuation: Continuation<GeminiFortuneResponse>,
        val timestamp: Long = System.currentTimeMillis(),
        val fortuneRepository: FortuneRepository
    )

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

    suspend fun generateFortune(
        userId: String,
        request: GeminiFortuneRequest,
        fortuneRepository: FortuneRepository
    ): GeminiFortuneResponse =
        suspendCoroutine { continuation ->
            val batchRequest = BatchRequest(userId, request, continuation, fortuneRepository = fortuneRepository)
            pendingRequests.offer(batchRequest)
        }

    private suspend fun processBatch() {
        val batch = mutableListOf<BatchRequest>()

        repeat(1) {
            pendingRequests.poll()?.let { batch.add(it) }
        }

        if (batch.isEmpty()) return

        val singleRequest = batch[0]
        logger.info("배치 처리 시작: userId=${singleRequest.userId}")

        try {
            val response = geminiService.generateFortuneInternal(singleRequest.request)
            val monthlyFortune = response.toMonthlyFortune()

            singleRequest.fortuneRepository.upsertMonthlyFortune(singleRequest.userId, monthlyFortune)
            singleRequest.continuation.resume(response)
        } catch (e: Exception) {
            batch.forEach { it.continuation.resumeWithException(e) }
        }
    }
}