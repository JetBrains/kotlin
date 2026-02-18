/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestRunnerType
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NativeBoxRunner(testServices: TestServices) : NativeBinaryArtifactHandler(testServices) {
    private var artifact: BinaryArtifacts.Native? = null
    override fun processModule(module: TestModule, info: BinaryArtifacts.Native) {
        if (NativeEnvironmentConfigurator.isMainModule(module, testServices.moduleStructure)) {
            if (artifact != null)
                error("Internal error: more than one executable for the testcase: ${artifact!!.executable.name} and ${info.executable.name}\n" +
                            "Only one module may have no incoming dependencies")
            artifact = info
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val executable = artifact?.executable ?: error("One main module is expected to be in the test.")
        val testRun = createTestRun(executable)
        val testRunner = createProperTestRunner(testRun, testServices.testRunSettings)
        testRunner.run()
    }

    private fun createTestRun(executable: File): TestRun {
        val checks = TestRunChecks(
            executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(30.seconds),
            testFiltering = TestRunCheck.TestFiltering(TestOutputFilter.NO_FILTERING),
            exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
        val testRun = TestRun(
            displayName = executable.name,
            executable = TestExecutable(
                TestCompilationArtifact.Executable(
                    executableFile = executable,
                    fileCheckStage = null,
                    hasSyntheticAccessorsDump = false,
                ),
                loggedCompilationToolCall = LoggedData.dummyCompilerCall,
                testNames = listOf(TestName(executable.name)),
            ),
            runParameters = emptyList(),
            testCase = TestCase(
                id = TestCaseId.Named(executable.name),
                kind = TestKind.STANDALONE,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs(),
                nominalPackageName = PackageName.EMPTY,
                checks = checks,
                extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
                fileCheckStage = null,
                expectedFailure = false,
            ),
            checks = checks,
            expectedFailure = false,
        )
        return testRun
    }
}