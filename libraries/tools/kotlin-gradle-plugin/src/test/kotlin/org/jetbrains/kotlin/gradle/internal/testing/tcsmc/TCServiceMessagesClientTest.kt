package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import org.gradle.internal.operations.OperationIdentifier
import org.jetbrains.kotlin.gradle.internal.testing.RecordingTestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

open class TCServiceMessagesClientTest {
    protected var rootNodeName: String = "root"
    protected var nameOfRootSuiteToAppend: String? = null
    protected var nameOfRootSuiteToReplace: String? = null
    protected var nameOfLeafTestToAppend: String? = null
    protected var skipRoots: Boolean = false
    protected var treatFailedTestOutputAsStacktrace: Boolean = false

    internal fun assertEvents(assertion: String, produceServiceMessage: TCServiceMessagesClient.() -> Unit) {
        val results = RecordingTestResultProcessor()
        val client = createClient(results)

        client.root(OperationIdentifier(1)) {
            client.produceServiceMessage()
        }

        assertEquals(
            assertion.trimTrailingWhitespaces().trim(),
            results.output.toString().trimTrailingWhitespaces().trim()
        )
    }

    internal open fun createClient(results: RecordingTestResultProcessor): TCServiceMessagesClient {
        return TCServiceMessagesClient(
            results,
            TCServiceMessagesClientSettings(
                rootNodeName,
                treatFailedTestOutputAsStacktrace = treatFailedTestOutputAsStacktrace
            ),
            LoggerFactory.getLogger("test")
        )
    }

    internal fun TCServiceMessagesClient.serviceMessage(name: String, attributes: Map<String, String>) =
        serviceMessage(ServiceMessage.parse(ServiceMessage.asString(name, attributes))!!)
}