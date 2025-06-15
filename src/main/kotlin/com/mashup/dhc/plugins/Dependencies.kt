package com.mashup.dhc.plugins

import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.domain.service.MissionPicker
import com.mashup.dhc.domain.service.TransactionService
import com.mashup.dhc.domain.service.UserService
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.application.Application
import io.ktor.server.config.tryGetString

data class Dependencies(
    val userService: UserService,
    val storage: NaverCloudPlatformObjectStorageAgent,
    val mongoClient: MongoClient,
    val mongoDatabase: MongoDatabase
)

fun Application.configureDependencies(): Dependencies {
    // MongoDB 설정
    val mongoConfig = getMongoConfig()
    val mongoClient = MongoClient.create(mongoConfig.uri)
    val mongoDatabase = mongoClient.getDatabase(mongoConfig.databaseName)

    // Repository 생성
    val userRepository = UserRepository(mongoDatabase)
    val missionRepository = MissionRepository(mongoDatabase)
    val pastRoutineHistoryRepository = PastRoutineHistoryRepository(mongoDatabase)

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

    // Storage 설정
    val storage =
        NaverCloudPlatformObjectStorageAgent(
            accessKey = environment.config.property("ncp.accessKey").getString(),
            secretKey = environment.config.property("ncp.secretKey").getString(),
            bucketName = environment.config.property("ncp.bucketName").getString()
        )

    return Dependencies(
        userService = userService,
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
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials =
        user
            ?.let { userVal ->
                password?.let { passwordVal -> "$userVal:$passwordVal@" }
            }.orEmpty()

    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    return MongoConfig(uri, databaseName)
}