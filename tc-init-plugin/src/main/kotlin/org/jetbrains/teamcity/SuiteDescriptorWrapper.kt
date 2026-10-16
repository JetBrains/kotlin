package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.TestDescriptor

class SuiteDescriptorWrapper(
    descriptor: TestDescriptor,
    flow: FlowWrapper,
    private val ignoredSuiteFormat: String?
) : DescriptorWrapper(descriptor, flow) {

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
        val suiteStartedMessage = ServiceMessage(
            name = "testSuiteStarted",
            attributes = mutableMapOf(
                "name" to descriptor.name,
                "flowId" to flowId
            )
        )
        action(flowMessage, suiteStartedMessage)
    }

    fun finish(action: (ServiceMessage?, ServiceMessage) -> Unit) {
        if (isIgnoredSuite) return
        val suiteFinishedMessage = ServiceMessage(
            name = "testSuiteFinished",
            attributes = mutableMapOf(
                "name" to descriptor.name,
                "flowId" to flowId
            )
        )
        var flowMessage: ServiceMessage? = null
        flow.finish(flowId) { serviceMessage ->
            flowMessage = serviceMessage
        }
        action(flowMessage, suiteFinishedMessage)
    }
}
