/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

internal class TestExecutable(
    val executableFile: File,
    val loggedCompilerCall: LoggedData.CompilerCall
)

internal class TestRun(
    val displayName: String,
    val executable: TestExecutable,
    val runParameters: List<TestRunParameter>,
    val testCaseId: TestCaseId
)

internal sealed interface TestRunParameter {
    fun applyTo(programArgs: MutableList<String>)

    sealed class WithFilter : TestRunParameter {
        abstract fun testMatches(testName: String): Boolean
    }

    class WithPackageFilter(packageName: PackageFQN) : WithFilter() {
        init {
            assertTrue(packageName.isNotEmpty())
        }

        private val packagePrefix = "$packageName."

        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$packagePrefix*"
        }

        override fun testMatches(testName: String) = testName.startsWith(packagePrefix)
    }

    class WithFunctionFilter(val testFunction: TestFunction) : WithFilter() {
        private val packagePrefix = if (testFunction.packageName.isNotEmpty()) "${testFunction.packageName}." else ""

        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_regex_filter=${packagePrefix.replace(".", "\\.")}([^\\.]+)\\.${testFunction.functionName}"
        }

        override fun testMatches(testName: String): Boolean {
            val remainder = if (packagePrefix.isNotEmpty()) {
                if (!testName.startsWith(packagePrefix)) return false
                testName.substringAfter(packagePrefix)
            } else
                testName

            val suffix = remainder
                .split('.')
                .takeIf { it.size == 2 }
                ?.last()
                ?: return false

            return suffix == testFunction.functionName
        }
    }

    object WithGTestLogger : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_logger=GTEST"
        }
    }

    class WithInputData(val inputDataFile: File) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) = Unit
    }

    class WithExpectedOutputData(val expectedOutputDataFile: File) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) = Unit
    }
}

internal inline fun <reified T : TestRunParameter> List<TestRunParameter>.has(): Boolean =
    firstIsInstanceOrNull<T>() != null

internal inline fun <reified T : TestRunParameter> List<TestRunParameter>.get(onFound: T.() -> Unit) {
    firstIsInstanceOrNull<T>()?.let(onFound)
}
