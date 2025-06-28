package com.mashup.dhc.utils

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

data class TestTask(
    override val taskId: String,
    val payload: String = "test"
) : BatchTask {
    override val taskType: String = "TestTask"
}

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleBatchProcessorTest {

    @Test
    fun `배치 크기 10개 도달시 즉시 GeminiAPI 호출`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 10개 작업 추가
        repeat(10) {
            batchProcessor.addTask(TestTask("task$it"))
        }
        advanceUntilIdle()

        // Then: GeminiAPI가 1회 호출되어야 한다.
        coVerify(exactly = 1) { batchExecutor(any()) }
        coVerify { batchExecutor(match { it.size == 10 }) }
        assertEquals(0, batchProcessor.getPendingCount(), "대기 중인 작업이 없어야 한다.")
    }

    @Test
    fun `4초 타임아웃시 남은 작업들 GeminiAPI 호출`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 5개 작업 추가 후 타임아웃 대기
        repeat(5) {
            batchProcessor.addTask(TestTask("task$it"))
        }

        assertEquals(5, batchProcessor.getPendingCount(), "5개 작업이 대기 중이어야 한다.")

        // 4초 타임아웃 트리거
        advanceTimeBy(4100L)
        advanceUntilIdle()

        // Then: GeminiAPI가 정확히 1회 호출되어야 한다.
        coVerify(exactly = 1) { batchExecutor(any()) }
        coVerify { batchExecutor(match { it.size == 5 }) }
        assertEquals(0, batchProcessor.getPendingCount(), "대기 중인 작업이 없어야 한다.")
    }

    @Test
    fun `여러 배치가 연속으로 처리되는 경우`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 첫 번째 배치 (10개)
        repeat(10) {
            batchProcessor.addTask(TestTask("batch1_task$it"))
        }
        advanceUntilIdle()

        // 두 번째 배치 (15개 -> 10개 + 5개로 분할)
        repeat(15) {
            batchProcessor.addTask(TestTask("batch2_task$it"))
        }
        advanceUntilIdle() // 10개짜리 배치 처리

        advanceTimeBy(4100L) // 타임아웃으로 5개 처리
        advanceUntilIdle()

        // Then: 총 3회 호출되어야 한다. (10개 + 10개 + 5개)
        coVerify(exactly = 3) { batchExecutor(any()) }
        assertEquals(0, batchProcessor.getPendingCount())
    }

    @Test
    fun `배치 상태 정보가 정확하게 반환됨`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 7개 작업 추가
        repeat(7) {
            batchProcessor.addTask(TestTask("task$it"))
        }

        val status = batchProcessor.getBatchStatus()

        // Then
        assertEquals(7, status.pendingTasks, "대기 중인 작업 수가 정확해야 한다.")
        assertEquals(10, status.batchSizeThreshold, "배치 크기 임계값이 정확해야 한다.")
        assertEquals(mapOf("TestTask" to 7), status.taskTypeDistribution, "작업 타입 분포가 정확해야 한다.")
    }

    @Test
    fun `배치 크기보다 적은 작업 추가시 즉시 실행되지 않음`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 9개 작업만 추가 (배치 크기 미달)
        repeat(9) {
            batchProcessor.addTask(TestTask("task$it"))
        }
        advanceUntilIdle()

        // Then: 즉시 실행되지 않아야 한다.
        coVerify(exactly = 0) { batchExecutor(any()) }
        assertEquals(9, batchProcessor.getPendingCount(), "9개 작업이 대기 중이어야 한다.")
    }

    @Test
    fun `동시에 많은 작업 추가시 올바른 배치 분할`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 25개 작업 한 번에 추가
        repeat(25) {
            batchProcessor.addTask(TestTask("task$it"))
        }
        advanceUntilIdle()

        // 남은 작업 타임아웃 처리
        advanceTimeBy(4100L)
        advanceUntilIdle()

        // Then: 3회 호출되어야 한다. (10개 + 10개 + 5개)
        coVerify(exactly = 3) { batchExecutor(any()) }
        assertEquals(0, batchProcessor.getPendingCount())
    }

    @Test
    fun `타임아웃 중간에 추가 작업이 들어와도 정확히 처리`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 3개 작업 추가
        repeat(3) {
            batchProcessor.addTask(TestTask("first_task$it"))
        }

        // 2초 후 2개 더 추가
        advanceTimeBy(2000L)
        repeat(2) {
            batchProcessor.addTask(TestTask("second_task$it"))
        }

        // 총 4초 경과하여 타임아웃 트리거
        advanceTimeBy(2100L)
        advanceUntilIdle()

        // 추가로 10개 작업 넣어서 즉시 실행
        repeat(10) {
            batchProcessor.addTask(TestTask("third_task$it"))
        }
        advanceUntilIdle()

        // Then: 2회 호출되어야 한다. (5개 타임아웃 + 10개 즉시)
        coVerify(exactly = 2) { batchExecutor(any()) }
        assertEquals(0, batchProcessor.getPendingCount())
    }

    @Test
    fun `빈 배치는 처리되지 않음`() = runTest {
        // Given
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk(relaxed = true)
        coEvery { batchExecutor(any()) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 작업을 추가하지 않고 타임아웃만 기다림
        advanceTimeBy(4100L)
        advanceUntilIdle()

        // Then: 호출되지 않아야 한다.
        coVerify(exactly = 0) { batchExecutor(any()) }
        assertEquals(0, batchProcessor.getPendingCount())
    }

    @Test
    fun `정확히 배치 크기만큼만 처리됨을 검증`() = runTest {
        // Given
        val capturedBatches = mutableListOf<List<TestTask>>()
        val batchExecutor: suspend (List<TestTask>) -> Unit = mockk()
        coEvery { batchExecutor(capture(capturedBatches)) } returns Unit

        val batchProcessor = BatchProcessor(
            backgroundScope = this,
            batchSize = 10,
            batchTimeoutMs = 4000L,
            batchExecutor = batchExecutor
        )

        // When: 23개 작업 추가
        repeat(23) {
            batchProcessor.addTask(TestTask("task$it"))
        }
        advanceUntilIdle()

        // 남은 작업 타임아웃 처리
        advanceTimeBy(4100L)
        advanceUntilIdle()

        // Then: 정확한 배치 크기로 처리되었는지 검증
        assertEquals(3, capturedBatches.size, "3개의 배치가 처리되어야 한다.")
        assertEquals(10, capturedBatches[0].size, "첫 번째 배치는 10개")
        assertEquals(10, capturedBatches[1].size, "두 번째 배치는 10개")
        assertEquals(3, capturedBatches[2].size, "세 번째 배치는 3개")
    }
}