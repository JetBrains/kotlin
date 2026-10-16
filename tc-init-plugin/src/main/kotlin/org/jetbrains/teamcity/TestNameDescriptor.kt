package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.TestDescriptor

class TestNameDescriptor(
    private val testNameFormat: String?
) {
    fun getTestName(descriptor: TestDescriptor): String {
        val format = testNameFormat ?: "name"
        return when (format) {
            "name" -> descriptor.name
            "displayName" -> descriptor.displayName
            else -> descriptor.name
        }
    }
}
