package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class TestDescriptorWrapper(
    descriptor: TestDescriptor,
    private val testNameDescriptor: TestNameDescriptor,
    flow: FlowWrapper,
    private val buildDirectoriesResolver: BuildDirectoriesResolver,
    private val ignoredSuiteFormat: String?
) : DescriptorWrapper(descriptor, flow) {

    private val name: String
        get() = testNameDescriptor.getTestName(descriptor)

    private val minAttachedTestException: Int
        get() = Integer.getInteger(Constants.MIN_ATTACHED_TEST_EXCEPTION_SYSTEM_PROP, 1024 * 1024)

    private val stackTraceMaxLength: Int
        get() = Integer.getInteger(Constants.STACKTRACE_MAX_LENGTH_SYSTEM_PROP, 16 * 1024)

    private val messageMaxLength: Int
        get() = Integer.getInteger(Constants.MESSAGE_MAX_LENGTH_SYSTEM_PROP, 16 * 1024)

    private val stackTraceLogDir: String?
        get() = System.getenv(Constants.STACKTRACE_LOG_DIR_ENV_VAR)

    val isIgnored: Boolean
        get() = descriptor.className == null

    private val isIgnoredSuite: Boolean
        get() = !ignoredSuiteFormat.isNullOrEmpty() &&
                descriptor.className == null &&
                descriptor.displayName.startsWith("Gradle Suite")

    fun start(action: (ServiceMessage?, ServiceMessage) -> Unit) {
        if (isIgnoredSuite) return
        var flowMessage: ServiceMessage? = null
        flow.start(flowId) { serviceMessage ->
            flowMessage = serviceMessage
        }
        val testStartedMessage = ServiceMessage(
            name = "testStarted",
            attributes = mutableMapOf(
                "name" to name,
                "captureStandardOutput" to "false",
                "flowId" to flowId
            )
        )
        action(flowMessage, testStartedMessage)
    }

    fun failure(result: TestResult, taskPath: String, action: (ServiceMessage) -> Unit) {
        if (isIgnoredSuite) return
        var message = ""
        var details = ""
        if (result.exceptions.isNotEmpty()) {
            val exception = result.exceptions[0]
            val stackTrace = getStackTrace(exception)
            message = exception.message ?: ""
            details = if (stackTrace.length > stackTraceMaxLength) {
                val reportFile = getReportFile(taskPath)
                if (stackTrace.length > minAttachedTestException && reportFile != null) {
                    reportFile.parentFile?.mkdirs()
                    reportFile.writeText(stackTrace)
                    val reportServiceMessage = ServiceMessage(
                        name = "message",
                        attributes = mutableMapOf(
                            "text" to "The stacktrace is too long and was written to the file '$reportFile'",
                            "status" to "WARNING",
                            "flowId" to flowId
                        )
                    )
                    action(reportServiceMessage)
                }
                stackTrace.substring(0, stackTraceMaxLength)
            } else {
                stackTrace
            }
        }
        val testFailedMessage = ServiceMessage(
            name = "testFailed",
            attributes = mutableMapOf(
                "name" to name,
                "message" to message,
                "details" to details,
                "flowId" to flowId
            )
        )
        action(testFailedMessage)
    }

    fun finish(result: TestResult, action: (ServiceMessage, ServiceMessage?) -> Unit) {
        if (isIgnoredSuite) return
        val testFinishedMessage = when (result.resultType) {
            TestResult.ResultType.SKIPPED -> {
                ServiceMessage(
                    name = "testIgnored",
                    attributes = mutableMapOf(
                        "name" to name,
                        "flowId" to flowId
                    )
                )
            }
            else -> {
                ServiceMessage(
                    name = "testFinished",
                    attributes = mutableMapOf(
                        "name" to name,
                        "duration" to (result.endTime - result.startTime).toString(),
                        "flowId" to flowId
                    )
                )
            }
        }
        var flowMessage: ServiceMessage? = null
        flow.finish(flowId) { serviceMessage ->
            flowMessage = serviceMessage
        }
        action(testFinishedMessage, flowMessage)
    }

    fun flush(outputs: List<TestOutputEvent>?, action: (ServiceMessage) -> Unit) {
        if (outputs == null) return
        for (output in outputs) {
            val name = when (output.destination) {
                TestOutputEvent.Destination.StdOut -> "testStdOut"
                TestOutputEvent.Destination.StdErr -> "testStdErr"
                else -> null
            }
            if (name != null) {
                splitByLength(output.message, messageMaxLength).forEach { part ->
                    val serviceMessage = ServiceMessage(
                        name = name,
                        attributes = mutableMapOf(
                            "name" to this.name,
                            "out" to part,
                            "flowId" to flowId
                        )
                    )
                    action(serviceMessage)
                }
            }
        }
    }

    private fun getStackTrace(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }

    private fun getReportFile(taskPath: String): File? {
        val logDir = stackTraceLogDir
        if (!logDir.isNullOrEmpty()) {
            return File(logDir, "stacktrace_${taskPath.replace(':', '_')}_${flowId}.log")
        }
        val buildDir = buildDirectoriesResolver.resolve(taskPath)
        return if (buildDir != null) {
            File(buildDir, "reports/stacktraces/stacktrace_${taskPath.replace(':', '_')}_${flowId}.log")
        } else {
            null
        }
    }

    private fun splitByLength(string: String, maxLength: Int): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < string.length) {
            val end = (i + maxLength).coerceAtMost(string.length)
            result.add(string.substring(i, end))
            i += maxLength
        }
        return result
    }
}
