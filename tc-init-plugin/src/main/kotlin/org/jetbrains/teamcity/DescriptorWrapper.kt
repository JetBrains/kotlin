package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.TestDescriptor

abstract class DescriptorWrapper(
    protected val descriptor: TestDescriptor,
    protected val flow: FlowWrapper
) {
    protected val flowId: String
        get() = "${flow.id}-${descriptor.hashCode()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DescriptorWrapper) return false
        return descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }
}
