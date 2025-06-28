package com.mashup.dhc.utils

interface BatchTask {
    val taskId: String
    val taskType: String
}

interface LockableTask : BatchTask {
    val lockKey: String
}

interface UserTask : BatchTask {
    val userId: String
}