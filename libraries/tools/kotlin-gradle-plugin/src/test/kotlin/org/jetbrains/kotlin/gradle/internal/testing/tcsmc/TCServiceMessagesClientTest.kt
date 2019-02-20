package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import org.gradle.internal.operations.OperationIdentifier
import org.jetbrains.kotlin.gradle.internal.testing.RecordingTestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

open class TCServiceMessagesClientTest {
    protected var nameOfRootSuiteToAppend: String? = null
    protected var nameOfRootSuiteToReplace: String? = null
    protected var nameOfLeafTestToAppend: String? = null
    protected var skipRoots: Boolean = false

    internal fun assertEvents(assertion: String, produceServiceMessage: TCServiceMessagesClient.() -> Unit) {
        val results = RecordingTestResultProcessor()
        val client = createClient(results)

        client.root(OperationIdentifier(1)) {
            client.produceServiceMessage()
        }

        assertEquals(
                assertion.trim(),
                results.output.toString().trim()
        )
    }

    internal open fun createClient(results: RecordingTestResultProcessor): TCServiceMessagesClient {
        return TCServiceMessagesClient(
                results,
                TCServiceMessagesClientSettings(
                        "root",
                        nameOfRootSuiteToAppend,
                        nameOfRootSuiteToReplace,
                        nameOfLeafTestToAppend,
                        skipRoots
                ),
                LoggerFactory.getLogger("test")
        )
    }
}