package com.mashup.dhc.utils

import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * BatchTask 인터페이스를 구현한 작업들을 배치로 처리
 */
class BatchProcessor<T : BatchTask>(
    private val backgroundScope: CoroutineScope,
    private val batchSize: Int = 10,
    private val batchTimeoutMs: Long = 4000L,
    private val batchExecutor: suspend (List<T>) -> Unit
) {
    private val log = LoggerFactory.getLogger(BatchProcessor::class.java)

    private val pendingTasks = ConcurrentLinkedQueue<T>()
    private val batchMutex = Mutex()
    private var timeoutJob: Job? = null

    /**
     * 작업을 배치 큐에 추가
     */
    fun addTask(task: T) {
        pendingTasks.offer(task)
        checkAndProcessBatch()
    }

    /**
     * 현재 대기 중인 작업 수 반환
     */
    fun getPendingCount(): Int = pendingTasks.size

    /**
     * 배치 처리 조건을 확인하고 필요시 처리 실행
     * 1. batchSize만큼 차면 즉시 처리
     * 2. 첫 번째 작업 추가 시 타이머 시작
     */
    private fun checkAndProcessBatch() {
        backgroundScope.launch {
            batchMutex.withLock {
                val currentSize = pendingTasks.size

                when {
                    // 조건 1: 배치 크기에 도달 - 즉시 처리
                    currentSize >= batchSize -> {
                        cancelTimeoutAndProcess()
                    }

                    // 조건 2: 첫 번째 작업이면서 타이머가 없으면 - 타이머 시작
                    currentSize == 1 && timeoutJob == null -> {
                        startBatchTimeout()
                    }

                    // 그 외의 경우: 기존 타이머가 처리할 예정
                    else -> {
                    }
                }
            }
        }
    }

    /**
     * 타임아웃 타이머 시작
     */
    private fun startBatchTimeout() {
        timeoutJob =
            backgroundScope.launch {
                delay(batchTimeoutMs)

                batchMutex.withLock {
                    if (pendingTasks.isNotEmpty()) {
                        val taskTypes = pendingTasks.groupBy { it.taskType }.mapValues { it.value.size }
                        log.info(
                            "Batch timeout (${batchTimeoutMs}ms) reached. Processing ${pendingTasks.size} tasks: $taskTypes"
                        )
                        processBatch()
                    }
                    timeoutJob = null
                }
            }
    }

    /**
     * 타이머 취소하고 즉시 배치 처리
     */
    private suspend fun cancelTimeoutAndProcess() {
        timeoutJob?.cancel()
        timeoutJob = null
        processBatch()
    }

    /**
     * 현재 대기 중인 작업들을 배치로 처리
     */
    private suspend fun processBatch() {
        val batch = mutableListOf<T>()

        // 큐에서 배치 크기만큼 작업을 가져옴
        repeat(batchSize) {
            val task = pendingTasks.poll()
            if (task != null) {
                batch.add(task)
            } else {
                return@repeat
            }
        }

        if (batch.isNotEmpty()) {
            val taskTypes = batch.groupBy { it.taskType }.mapValues { it.value.size }
            log.info("Processing batch of ${batch.size} tasks: $taskTypes")
            val startTime = System.currentTimeMillis()

            try {
                batchExecutor(batch)
                val duration = System.currentTimeMillis() - startTime
                log.info("Batch completed! Processed ${batch.size} tasks in ${duration}ms")
            } catch (e: Exception) {
                log.error("Batch processing failed", e)
                throw e
            }

            // 남은 작업이 있으면 다음 배치를 위한 타이머 시작
            if (pendingTasks.isNotEmpty()) {
                log.info("Remaining tasks: ${pendingTasks.size}. Starting next batch timer.")
                startBatchTimeout()
            }
        }
    }

    /**
     * 배치 상태 정보 반환
     */
    fun getBatchStatus(): BatchStatus {
        val pendingCount = pendingTasks.size
        val isTimerActive = timeoutJob?.isActive == true
        val taskTypeDistribution = pendingTasks.groupBy { it.taskType }.mapValues { it.value.size }

        return BatchStatus(
            pendingTasks = pendingCount,
            batchSizeThreshold = batchSize,
            isTimeoutActive = isTimerActive,
            taskTypeDistribution = taskTypeDistribution,
            nextProcessCondition =
                when {
                    pendingCount >= batchSize -> "즉시 처리 (배치 크기 도달)"
                    isTimerActive -> "타이머 대기 중 (최대 ${batchTimeoutMs}ms)"
                    pendingCount > 0 -> "다음 요청 시 타이머 시작"
                    else -> "대기 중인 요청 없음"
                }
        )
    }
}

/**
 * 배치 처리 상태 정보
 */
data class BatchStatus(
    val pendingTasks: Int,
    val batchSizeThreshold: Int,
    val isTimeoutActive: Boolean,
    val taskTypeDistribution: Map<String, Int>,
    val nextProcessCondition: String
)