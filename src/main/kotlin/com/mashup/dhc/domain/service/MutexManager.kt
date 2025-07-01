package com.mashup.dhc.domain.service

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex

class MutexManager(
    private val cleanupTimeoutMs: Long = 5 * 60 * 1000L
) {
    private data class MutexEntry(
        val mutex: Mutex,
        @Volatile var lastUsed: Long
    )

    private val mutexMap = ConcurrentHashMap<String, MutexEntry>()

    init {
        // 주기적으로 오래된 lock 제거 (백그라운드 thread)
        startCleanupThread()
    }

    suspend fun <T> withLock(
        key: String,
        block: suspend () -> T
    ): T {
        val mutexEntry =
            mutexMap.computeIfAbsent(key) {
                MutexEntry(Mutex(), System.currentTimeMillis())
            }
        mutexEntry.lastUsed = System.currentTimeMillis()
        return mutexEntry.mutex.tryWithLock {
            mutexEntry.lastUsed = System.currentTimeMillis()
            block()
        } ?: throw RuntimeException("Lock key is using")
    }

    private fun startCleanupThread() {
        Thread {
            while (true) {
                Thread.sleep(cleanupTimeoutMs / 2)
                val now = System.currentTimeMillis()
                mutexMap.entries.removeIf { (_, entry) ->
                    !entry.mutex.isLocked && now - entry.lastUsed > cleanupTimeoutMs
                }
            }
        }.apply {
            isDaemon = true
            name = "MutexManager-Cleanup"
        }.start()
    }

    private suspend fun <T> Mutex.tryWithLock(block: suspend () -> T): T? {
        if (this.tryLock()) {
            try {
                return block()
            } finally {
                this.unlock()
            }
        }
        return null // 락 획득 실패 시 null 반환
    }
}