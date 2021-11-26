/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.AbstractRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.LocalTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeCache
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

internal class TestRunProvider(
    private val environment: TestEnvironment,
    private val testCaseGroupProvider: TestCaseGroupProvider
) : ExtensionContext.Store.CloseableResource {
    private val compilationFactory = TestCompilationFactory(environment)
    private val cachedCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation>()

    fun getSingleTestRun(testDataFile: File): TestRun {
        environment.assertNotDisposed()

        val testDataDir = testDataFile.parentFile
        val testDataFileName = testDataFile.name

        val testCaseGroup = testCaseGroupProvider.getTestCaseGroup(testDataDir) ?: fail { "No test case for $testDataFile" }
        assumeTrue(testCaseGroup.isEnabled(testDataFileName), "Test case is disabled")

        val testCase = testCaseGroup.getByName(testDataFileName) ?: fail { "No test case for $testDataFile" }

        val testCompilation = when (testCase.kind) {
            TestKind.STANDALONE, TestKind.STANDALONE_NO_TR -> {
                // Create a separate compilation for each standalone test case.
                cachedCompilations.computeIfAbsent(TestCompilationCacheKey.Standalone(testDataFile)) {
                    compilationFactory.testCasesToExecutable(listOf(testCase))
                }
            }
            TestKind.REGULAR -> {
                // Group regular test cases by compiler arguments.
                cachedCompilations.computeIfAbsent(TestCompilationCacheKey.Grouped(testDataDir, testCase.freeCompilerArgs)) {
                    val testCases = testCaseGroup.getRegularOnlyByCompilerArgs(testCase.freeCompilerArgs)
                    assertTrue(testCases.isNotEmpty())
                    compilationFactory.testCasesToExecutable(testCases)
                }
            }
        }

        val (executableFile, loggedCompilerCall) = testCompilation.result.assertSuccess() // <-- Compilation happens here.
        val executable = TestExecutable(executableFile, loggedCompilerCall)

        val runParameters = when (testCase.kind) {
            TestKind.STANDALONE_NO_TR -> listOfNotNull(
                testCase.extras!!.inputDataFile?.let(TestRunParameter::WithInputData),
                testCase.expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
            TestKind.STANDALONE -> listOfNotNull(
                TestRunParameter.WithGTestLogger,
                testCase.expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
            TestKind.REGULAR -> listOfNotNull(
                TestRunParameter.WithGTestLogger,
                TestRunParameter.WithPackageFilter(testCase.nominalPackageName),
                testCase.expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
        }

        return TestRun(executable, runParameters, testCase.origin)
    }

    // Currently, only local test runner is supported.
    fun createRunner(testRun: TestRun): AbstractRunner<*> = when (val target = environment.globalEnvironment.target) {
        environment.globalEnvironment.hostTarget -> LocalTestRunner(testRun, environment.globalEnvironment.executionTimeout)
        else -> fail {
            """
                Running at non-host target is not supported yet.
                Compilation target: $target
                Host target: ${environment.globalEnvironment.hostTarget}
            """.trimIndent()
        }
    }

    override fun close() {
        Disposer.dispose(environment)
    }

    private sealed class TestCompilationCacheKey {
        data class Standalone(val testDataFile: File) : TestCompilationCacheKey()
        data class Grouped(val testDataDir: File, val freeCompilerArgs: TestCompilerArgs) : TestCompilationCacheKey()
    }
}
