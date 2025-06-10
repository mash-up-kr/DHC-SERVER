package com.mashup.com.mashup.dhc.domain.service

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex

//TODO Guava + ReentrantLock 조합으로 변경하기 (Lock 만료)
object LockRegistry {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun tryLock(lockKey: String): Boolean {
        val mutex = locks.computeIfAbsent(lockKey) { Mutex() }
        return mutex.tryLock()
    }

    fun unlock(lockKey: String) {
        locks[lockKey]?.unlock()
    }

    suspend fun finallyUnlock(lockKey: String, operations: suspend () -> Unit) {
        try {
            operations()
        } finally {
            unlock(lockKey)
        }
    }
}
