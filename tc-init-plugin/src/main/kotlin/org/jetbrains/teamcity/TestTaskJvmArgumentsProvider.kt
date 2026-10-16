package org.jetbrains.teamcity

import org.gradle.process.CommandLineArgumentProvider

class TestTaskJvmArgumentsProvider(
    private val jvmArgs: List<String>
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        return jvmArgs
    }
}
