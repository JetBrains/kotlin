/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.konan.blackboxtest.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.group.TestCaseGroupProvider
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class TestRunProvider(
    private val environment: TestEnvironment,
    private val testCaseGroupProvider: TestCaseGroupProvider
) : ExtensionContext.Store.CloseableResource {
    private val compilationFactory = TestCompilationFactory(environment)
    private val cachedCompilations: MutableMap<TestCompilationCacheKey, TestCompilation> = ConcurrentHashMap()

    fun getSingleTestRunForTestDataFile(testDataFile: File): TestRun {
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
        val executable = TestExecutable(executableFile, testCase.origin, loggedCompilerCall)

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
                TestRunParameter.WithPackageName(packageName = testCase.nominalPackageName),
                testCase.expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
        }

        return TestRun(executable, runParameters)
    }

    override fun close() {
        Disposer.dispose(environment)
    }

    private sealed class TestCompilationCacheKey {
        data class Standalone(val testDataFile: File) : TestCompilationCacheKey()
        data class Grouped(val testDataDir: File, val freeCompilerArgs: TestCompilerArgs) : TestCompilationCacheKey()
    }
}
