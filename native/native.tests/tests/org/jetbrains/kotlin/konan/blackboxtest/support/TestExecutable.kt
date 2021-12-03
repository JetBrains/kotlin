/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.support.util.startsWith
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
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
        abstract fun testMatches(testName: TestName): Boolean
    }

    class WithPackageFilter(private val packageName: PackageName) : WithFilter() {
        init {
            assertFalse(packageName.isEmpty())
        }

        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$packageName.*"
        }

        override fun testMatches(testName: TestName) = testName.packageName.startsWith(packageName)
    }

    class WithTestFilter(val testName: TestName) : WithFilter() {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$testName"
        }

        override fun testMatches(testName: TestName) = this.testName == testName
    }

    object WithTCTestLogger : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_logger=TEAMCITY"
            programArgs += "--ktest_no_exit_code"
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
