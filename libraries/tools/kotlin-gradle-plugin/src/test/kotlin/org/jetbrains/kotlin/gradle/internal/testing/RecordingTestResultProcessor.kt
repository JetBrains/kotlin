/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import org.gradle.api.internal.tasks.testing.*
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.api.tasks.testing.TestOutputEvent

class RecordingTestResultProcessor : TestResultProcessor {
    val output = StringBuilder()
    var indent = 0

    fun line(line: String) {
        repeat(indent) { output.append("  ") }
        output.append(line).append("\n")
    }

    override fun started(test: TestDescriptorInternal, event: TestStartEvent) {
        val description = when (test) {
            is DefaultTestSuiteDescriptor -> "SUITE ${test.displayName}"
            is DefaultTestDescriptor -> "TEST displayName: ${test.displayName}, " +
                    "classDisplayName: ${test.classDisplayName}, " +
                    "className: ${test.className}, " +
                    "name: ${test.name}"
            else -> error("Unknown test descriptor $test")
        }
        line("STARTED $description // ${test.id}")
        indent++
    }

    override fun output(testId: Any, event: TestOutputEvent) {
        line("${event.destination}[${event.message}] // $testId")
    }

    override fun failure(testId: Any?, result: TestFailure?) {
        line("FAILURE ${result?.rawFailure} // $testId")
    }

    override fun completed(testId: Any, event: TestCompleteEvent) {
        indent--
        line("COMPLETED ${event.resultType} // $testId")
    }
}
