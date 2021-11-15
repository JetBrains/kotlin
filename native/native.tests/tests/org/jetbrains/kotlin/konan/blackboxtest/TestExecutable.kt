/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

internal class TestExecutable(
    val executableFile: File,
    val loggedCompilerCall: LoggedData.CompilerCall
)

internal class TestRun(
    val executable: TestExecutable,
    val runParameters: List<TestRunParameter>,
    val origin: TestOrigin.SingleTestDataFile
)

internal sealed interface TestRunParameter {
    fun applyTo(programArgs: MutableList<String>)

    sealed class WithFilter : TestRunParameter {
        protected abstract val wildcard: String
        abstract fun testMatches(testName: String): Boolean

        final override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$wildcard"
        }
    }

    class WithPackageFilter(private val packageFQN: PackageName) : WithFilter() {
        override val wildcard get() = "$packageFQN.*"
        override fun testMatches(testName: String) = testName.startsWith("$packageFQN.")
    }

/*
    class WithFunctionFilter(private val functionFQN: String) : WithFilter() {
        override val wildcard get() = functionFQN
        override fun testMatches(testName: String) = testName == functionFQN
    }
*/

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
