package com.mashup.dhc.domain.model

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.junit.After
import org.junit.Before

/**
 * Base test class for MongoDB repository tests.
 * Provides common setup and teardown functionality for MongoDB-based tests.
 */
abstract class BaseMongoDBTest {
    private lateinit var mongoExtension: EmbeddedMongoDBExtension
    protected lateinit var database: MongoDatabase

    @Before
    fun setUpMongoDB() {
        // Setup embedded MongoDB
        mongoExtension = EmbeddedMongoDBExtension()
        mongoExtension.start()

        // Get database
        database = mongoExtension.getDatabase()

        // Setup repositories and other test-specific components
        setUp()
    }

    @After
    fun tearDownMongoDB() {
        // Clean up test-specific components
        tearDown()

        // Stop MongoDB
        mongoExtension.stop()
    }

    /**
     * Template method to be implemented by subclasses for specific setup.
     */
    protected abstract fun setUp()

    /**
     * Template method to be implemented by subclasses for specific teardown.
     * Default implementation does nothing.
     */
    protected open fun tearDown() {
        // Default implementation does nothing
    }
}