package com.mashup.dhc

import com.mashup.com.mashup.dhc.domain.model.PastRoutineHistoryRepository
import com.mashup.com.mashup.dhc.domain.service.MissionPicker
import com.mashup.com.mashup.dhc.domain.service.UserService
import com.mashup.com.mashup.dhc.plugins.configureRouting
import com.mashup.dhc.domain.model.MissionRepository
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.domain.service.TransactionService
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mashup.dhc.external.NaverCloudPlatformObjectStorageAgent
import com.mashup.dhc.routes.sampleRoute
import com.mashup.dhc.routes.storageRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.config.tryGetString
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority" // TODO

    val mongoClient: MongoClient = MongoClient.create(uri)
    val mongoDatabase: MongoDatabase = mongoClient.getDatabase(databaseName)

    val userService =
        UserService(
            transactionService = TransactionService(mongoClient),
            userRepository = UserRepository(mongoDatabase),
            missionRepository = MissionRepository(mongoDatabase),
            pastRoutineHistoryRepository = PastRoutineHistoryRepository(mongoDatabase),
            missionPicker = MissionPicker(MissionRepository(mongoDatabase))
        )
    configureRouting(userService)
    // JSON Serial/Deserial 자동화
    install(ContentNegotiation) {
        json()
    }

    val storage =
        NaverCloudPlatformObjectStorageAgent(
            accessKey = environment.config.property("ncp.accessKey").getString(),
            secretKey = environment.config.property("ncp.secretKey").getString(),
            bucketName = environment.config.property("ncp.bucketName").getString()
        )

    configureRouting()

    routing {
        sampleRoute()
        storageRoutes(storage)
    }
}