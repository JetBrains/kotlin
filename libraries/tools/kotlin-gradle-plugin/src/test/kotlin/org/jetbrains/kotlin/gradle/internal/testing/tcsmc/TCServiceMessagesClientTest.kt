package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.internal.operations.OperationIdentifier
import org.jetbrains.kotlin.gradle.internal.testing.RecordingTestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.plugin.internal.MppTestReportHelper
import org.jetbrains.kotlin.gradle.testing.KotlinTestFailure
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
            LoggerFactory.getLogger("test"),
            object : MppTestReportHelper {
                override fun reportFailure(results: TestResultProcessor, id: Any, failure: KotlinTestFailure, isAssertionFailure: Boolean) {
                    results.failure(
                        id,
                        if (isAssertionFailure) {
                            TestFailure.fromTestAssertionFailure(failure, failure.expected, failure.actual)
                        } else {
                            TestFailure.fromTestFrameworkFailure(failure)
                        }
                    )
                }

                override fun createDelegatingTestReportProcessor(origin: TestResultProcessor, targetName: String): TestResultProcessor =
                    origin
            }
        )
    }

    internal fun TCServiceMessagesClient.serviceMessage(name: String, attributes: Map<String, String>) =
        serviceMessage(ServiceMessage.parse(ServiceMessage.asString(name, attributes))!!)
}