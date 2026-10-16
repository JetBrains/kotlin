package org.jetbrains.teamcity

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.api.tasks.testing.TestResult
import java.util.concurrent.ConcurrentHashMap

class TeamcityTestListener(
    val taskPath: String,
    private val buildDirectoriesResolver: BuildDirectoriesResolver,
    private val testNameFormat: String?,
    private val ignoredSuiteFormat: String?,
    private val logger: Logger
) : TestListener, TestOutputListener {

    private val flowWrapper = FlowWrapper(Util.getFlowIdForTask(taskPath))
    private val testNameDescriptor = TestNameDescriptor(testNameFormat)
    private val testOutputs = ConcurrentHashMap<TestDescriptorWrapper, MutableList<TestOutputEvent>>()

    override fun beforeSuite(testDescriptor: TestDescriptor) {
        val suiteDescriptorWrapper = SuiteDescriptorWrapper(testDescriptor, flowWrapper, ignoredSuiteFormat)
        suiteDescriptorWrapper.start { flowMessage, suiteStartedMessage ->
            if (flowMessage != null) logger.lifecycle(flowMessage.asString())
            logger.lifecycle(suiteStartedMessage.asString())
        }
    }

    override fun afterSuite(testDescriptor: TestDescriptor, testResult: TestResult) {
        val suiteDescriptorWrapper = SuiteDescriptorWrapper(testDescriptor, flowWrapper, ignoredSuiteFormat)
        suiteDescriptorWrapper.finish { flowMessage, suiteFinishedMessage ->
            val testDescriptorWrapper = TestDescriptorWrapper(testDescriptor, testNameDescriptor, flowWrapper, buildDirectoriesResolver, ignoredSuiteFormat)
            val outputs = testOutputs.remove(testDescriptorWrapper)
            if (outputs != null) {
                synchronized(outputs) {
                    testDescriptorWrapper.flush(outputs) { testOutputServiceMessage ->
                        logger.lifecycle(testOutputServiceMessage.asString())
                    }
                    outputs.clear()
                }
            }
            logger.lifecycle(suiteFinishedMessage.asString())
            if (flowMessage != null) logger.lifecycle(flowMessage.asString())
        }
        if (testDescriptor.parent == null) {
            testOutputs.forEach { (key, outputs) ->
                synchronized(outputs) {
                    key.flush(outputs) { testOutputServiceMessage ->
                        logger.lifecycle(testOutputServiceMessage.asString())
                    }
                    outputs.clear()
                }
            }
            testOutputs.clear()
        }
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {
        val testDescriptorWrapper = TestDescriptorWrapper(testDescriptor, testNameDescriptor, flowWrapper, buildDirectoriesResolver, ignoredSuiteFormat)
        testDescriptorWrapper.start { flowMessage, testStartedMessage ->
            if (flowMessage != null) logger.lifecycle(flowMessage.asString())
            logger.lifecycle(testStartedMessage.asString())
        }
    }

    override fun afterTest(testDescriptor: TestDescriptor, testResult: TestResult) {
        val testDescriptorWrapper = TestDescriptorWrapper(testDescriptor, testNameDescriptor, flowWrapper, buildDirectoriesResolver, ignoredSuiteFormat)
        val outputs = testOutputs.remove(testDescriptorWrapper)
        if (outputs != null) {
            synchronized(outputs) {
                testDescriptorWrapper.flush(outputs) { testOutputServiceMessage ->
                    logger.lifecycle(testOutputServiceMessage.asString())
                }
                outputs.clear()
            }
        }
        if (testResult.failedTestCount > 0) {
            testDescriptorWrapper.failure(testResult, taskPath) { testFailedServiceMessage ->
                logger.lifecycle(testFailedServiceMessage.asString())
            }
        }
        testDescriptorWrapper.finish(testResult) { testFinishedMessage, flowMessage ->
            logger.lifecycle(testFinishedMessage.asString())
            if (flowMessage != null) logger.lifecycle(flowMessage.asString())
        }
    }

    override fun onOutput(testDescriptor: TestDescriptor, testOutputEvent: TestOutputEvent) {
        if (testDescriptor.className == null) {
            return
        }
        if (testOutputEvent.destination == TestOutputEvent.Destination.StdOut && java.lang.Boolean.getBoolean(Constants.IGNORE_TEST_STDOUT_SYSTEM_PROP)) {
            return
        }
        if (testOutputEvent.destination == TestOutputEvent.Destination.StdErr && java.lang.Boolean.getBoolean(Constants.IGNORE_TEST_STDERR_SYSTEM_PROP)) {
            return
        }
        val testDescriptorWrapper = TestDescriptorWrapper(testDescriptor, testNameDescriptor, flowWrapper, buildDirectoriesResolver, ignoredSuiteFormat)
        val outputs = testOutputs.computeIfAbsent(testDescriptorWrapper) { ArrayList() }
        synchronized(outputs) {
            outputs.add(testOutputEvent)
            if (outputs.size > 100) {
                testDescriptorWrapper.flush(outputs) { testOutputServiceMessage ->
                    logger.lifecycle(testOutputServiceMessage.asString())
                }
                outputs.clear()
            }
        }
    }
}
