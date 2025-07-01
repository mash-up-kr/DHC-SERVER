package com.mashup.dhc.plugins

import com.mashup.dhc.domain.model.FortuneRepository
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.domain.service.FortuneService
import com.mashup.dhc.domain.service.GeminiService
import com.mashup.dhc.domain.service.MissionPicker
import com.mashup.dhc.domain.service.TransactionService
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.tryGetString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

data class Dependencies(
    val userService: UserService,
    val fortuneService: FortuneService,
    val storage: NaverCloudPlatformObjectStorageAgent,
    val mongoClient: MongoClient,
    val mongoDatabase: MongoDatabase
)

fun Application.configureDependencies(): Dependencies {
    // backgroundScope 애플리케이션 전역에서 사용
    val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 서버 종료 시 backgroundScope 취소
    environment.monitor.subscribe(ApplicationStopped) {
        backgroundScope.cancel()
    }

    // 환경설정 값 읽기
    val geminiApiKey =
        environment.config.propertyOrNull("gemini.api.key")?.getString()
            ?: throw RuntimeException("Gemini API Key가 설정되지 않았습니다.")

    val systemInstruction =
        environment.config.propertyOrNull("gemini.api.instruction")?.getString()
            ?: throw RuntimeException("System Instruction이 설정되지 않았습니다.")

    // MongoDB 설정
    val mongoConfig = getMongoConfig()
    val mongoClient = MongoClient.create(mongoConfig.uri)
    val mongoDatabase = mongoClient.getDatabase(mongoConfig.databaseName)

    // Repository 생성
    val userRepository = UserRepository(mongoDatabase)
    val missionRepository = MissionRepository(mongoDatabase)
    val pastRoutineHistoryRepository = PastRoutineHistoryRepository(mongoDatabase)
    val fortuneRepository = FortuneRepository(mongoDatabase)

    // Service 생성
    val transactionService = TransactionService(mongoClient)
    val missionPicker = MissionPicker(missionRepository)
    val userService =
        UserService(
            transactionService = transactionService,
            userRepository = userRepository,
            missionRepository = missionRepository,
            pastRoutineHistoryRepository = pastRoutineHistoryRepository,
            missionPicker = missionPicker
        )
    val geminiService = GeminiService(geminiApiKey, systemInstruction, fortuneRepository)
    geminiService.startBatchProcessor(backgroundScope)

    val fortuneService =
        FortuneService(
            backgroundScope = backgroundScope,
            userService = userService,
            geminiService = geminiService,
            fortuneRepository = fortuneRepository
        )

    // Storage 설정
    val storage =
        NaverCloudPlatformObjectStorageAgent(
            accessKey = environment.config.property("ncp.accessKey").getString(),
            secretKey = environment.config.property("ncp.secretKey").getString(),
            bucketName = environment.config.property("ncp.bucketName").getString()
        )

    monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return Dependencies(
        userService = userService,
        fortuneService = fortuneService,
        storage = storage,
        mongoClient = mongoClient,
        mongoDatabase = mongoDatabase
    )
}

private data class MongoConfig(
    val uri: String,
    val databaseName: String
)

private fun Application.getMongoConfig(): MongoConfig {
    val host = environment.config.tryGetString("db.mongo.host") ?: "211.188.52.240"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "dhc"

    return MongoConfig("mongodb://$host:$port", databaseName)
}