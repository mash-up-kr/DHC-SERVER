package com.mashup.dhc.domain.model

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.MongodArguments
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.embed.process.io.ProcessOutput
import de.flapdoodle.embed.process.runtime.Network
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start

/**
 * Helper class for managing embedded MongoDB lifecycle in tests
 */
class EmbeddedMongoDBExtension(
    private val port: Int = 27018,
    private val dbName: String = "test-db"
) {
    private lateinit var mongodProcess: TransitionWalker.ReachedState<RunningMongodProcess>
    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase

    fun start() {
        // Configure and start MongoDB
        mongodProcess =
            Mongod
                .instance()
                .withNet(
                    Start
                        .to(Net::class.java)
                        .initializedWith(Net.of("localhost", port, Network.localhostIsIPv6()))
                ).withProcessOutput(
                    Start
                        .to(ProcessOutput::class.java)
                        .initializedWith(ProcessOutput.silent())
                ).withMongodArguments(
                    Start
                        .to(MongodArguments::class.java)
                        .initializedWith(
                            MongodArguments
                                .defaults()
                                .withArgs(mapOf())
                        )
                ).start(Version.Main.V6_0)

        // Connect to the running MongoDB instance
        mongoClient = MongoClient.create("mongodb://localhost:$port")
        mongoDatabase = mongoClient.getDatabase(dbName)
    }

    fun stop() {
        mongoClient.close()
        if (::mongodProcess.isInitialized) {
            mongodProcess.close()
        }
    }

    fun getDatabase(): MongoDatabase = mongoDatabase

    fun getClient(): MongoClient = mongoClient
}