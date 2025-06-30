package com.mashup.dhc.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test

data class TestTask(
    override val taskId: String,
    val payload: String = "test"
) : BatchTask {
    override val taskType: String = "TestTask"
}

class SimpleBatchProcessorTest {

    @Test
    fun `배치 크기 10개 도달시 즉시 호출`() {
        // Given
        val processedBatch = AtomicReference<List<TestTask>>(null)
        val latch = CountDownLatch(1)

        val batchExecutor: suspend (List<TestTask>) -> Unit = { batch ->
            processedBatch.set(batch)
            latch.countDown()
        }

        val batchProcessor = BatchProcessor(
            backgroundScope = CoroutineScope(Dispatchers.Default),
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 10개 작업 추가
        repeat(10) {
            batchProcessor.addTask(TestTask("task$it"))
        }

        // Then: 5초 내에 작업이 처리되어야한다
        val processed = latch.await(5, TimeUnit.SECONDS)
        assertEquals(true, processed, "5초 내에 배치가 처리되어야한다")
        assertEquals(10, processedBatch.get().size, "배치 크기는 10이어야한다")
        assertEquals(0, batchProcessor.getPendingCount(), "대기 중인 작업이 없어야한다")
    }

    @Test
    fun `4초 타임아웃시 남은 작업들 호출`() {
        // Given
        val processedBatch = AtomicReference<List<TestTask>>(null)
        val latch = CountDownLatch(1)

        val batchExecutor: suspend (List<TestTask>) -> Unit = { batch ->
            processedBatch.set(batch)
            latch.countDown()
        }

        val batchProcessor = BatchProcessor(
            backgroundScope = CoroutineScope(Dispatchers.Default),
            batchSize = 10,
            batchTimeoutMs = 1000L, // 1초 타임아웃 (테스트 시간 단축)
            batchExecutor = batchExecutor
        )

        // When: 5개 작업 추가 (배치 크기 미달)
        repeat(5) {
            batchProcessor.addTask(TestTask("task$it"))
        }

        // Then: 5초 내에 타임아웃으로 처리되어야한다
        val processed = latch.await(5, TimeUnit.SECONDS)
        assertEquals(true, processed, "5초 내에 배치가 타임아웃으로 처리되어야한다")
        assertEquals(5, processedBatch.get().size, "배치 크기는 5이어야한다")
        assertEquals(0, batchProcessor.getPendingCount(), "대기 중인 작업이 없어야한다")
    }

    @Test
    fun `배치 크기보다 적은 작업은 타임아웃 전까지 처리되지 않음`() {
        // Given
        val processedCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val batchExecutor: suspend (List<TestTask>) -> Unit = {
            processedCount.incrementAndGet()
            latch.countDown()
        }

        val batchProcessor = BatchProcessor(
            backgroundScope = CoroutineScope(Dispatchers.Default),
            batchSize = 10,
            batchTimeoutMs = 2000L, // 2초 타임아웃
            batchExecutor = batchExecutor
        )

        // When: 5개 작업 추가 (배치 크기 미달)
        repeat(5) {
            batchProcessor.addTask(TestTask("task$it"))
        }

        // 타임아웃 발생 전에 확인 (0.5초 후)
        Thread.sleep(500)
        assertEquals(0, processedCount.get(), "타임아웃 전에는 호출되지 않아야한다")
        assertEquals(5, batchProcessor.getPendingCount(), "5개 작업이 여전히 대기 중이어야한다")

        // 타임아웃 발생 후 확인 (충분한 시간 기다림)
        val processed = latch.await(5, TimeUnit.SECONDS)
        assertEquals(true, processed, "5초 내에 타임아웃으로 처리되어야한다")
        assertEquals(1, processedCount.get(), "타임아웃 후에는 호출되어야한다")
        assertEquals(0, batchProcessor.getPendingCount(), "처리 후 대기 중인 작업이 없어야한다")
    }
}