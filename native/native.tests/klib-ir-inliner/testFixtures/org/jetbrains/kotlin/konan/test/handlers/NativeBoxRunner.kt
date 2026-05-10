/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunParameter
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.*
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.computePackageName
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.native.executors.Executor
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.groupingPhaseInputs
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File
import kotlin.test.assertIs

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
        val testRun = createTestRun(
            executable,
            testServices,
            TestCaseId.TestDataFile(testServices.moduleStructure.originalTestDataFiles.first()),
            addTeamCityLogger = true,
            addTestFilter = true,
        )
        val testRunner = createProperTestRunner(testRun, testServices.testRunSettings)
        testRunner.run()
    }
}

class NativeBoxRunnerGroupingPhase(testServices: TestServices) : GroupingPhaseHandler<BinaryArtifacts.Native>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Native>
        get() = ArtifactKinds.Native

    override fun processArtifact(artifact: BinaryArtifacts.Native) {
        val executable = artifact.executable
        val testRun = createTestRun(
            executable,
            testServices,
            TestCaseId.Named("batch"),
            addTeamCityLogger = true,
            addTestFilter = false,
        )
        val testRunner = createProperTestRunner(testRun, testServices.testRunSettings) { executor, testRun ->
            RunnerWithExecutorAndPrettyHandler(executor, testRun, testServices) { e -> WrappedException.FromGroupingHandler(e, this) }
        }
        testRunner.run()
    }
}

private fun createTestRun(
    executable: File,
    testServices: TestServices,
    caseId: TestCaseId,
    addTeamCityLogger: Boolean,
    addTestFilter: Boolean,
): TestRun {
    val testKind = testServices.testRunSettings.testKind(testServices.moduleStructure.modules.firstOrNull()?.directives)
    val checks = TestRunChecks(
        executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(testServices.testRunSettings.get<Timeouts>().executionTimeout),
        testFiltering = TestRunCheck.TestFiltering(
            if (testKind in listOf(TestKind.REGULAR, TestKind.STANDALONE)) TCTestOutputFilter
            else TestOutputFilter.NO_FILTERING
        ),
        exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
        outputDataFile = null,
        outputMatcher = null,
        fileCheckMatcher = null,
    )
    val success = TestCompilationResult.Success(
        TestCompilationArtifact.Executable(
            executableFile = executable,
            fileCheckStage = null,
            hasSyntheticAccessorsDump = false,
        ),
        LoggedData.dummyCompilerCall, // TODO: populate at least real compiler invocation arguments
    )
    val testName = null // TODO: see `TestRunProvider.getTestRuns()`, how to generate test names
    val runParameters = getTestRunParameters(
        testKind, testName, checks, testServices,
        addTeamCityLogger = addTeamCityLogger,
        addTestFilter = addTestFilter
    )
    val testRun = TestRun(
        displayName = /* Unimportant. Used only in dynamic tests. */ "",
        executable = TestExecutable.fromCompilationResult(testKind, success),
        runParameters = runParameters,
        testCase = TestCase(
            id = caseId,
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

private fun getTestRunParameters(
    testKind: TestKind,
    testName: TestName?,
    checks: TestRunChecks,
    testServices: TestServices,
    addTeamCityLogger: Boolean,
    addTestFilter: Boolean,
): List<TestRunParameter> = buildList {
    when (testKind) {
        TestKind.STANDALONE -> {
            // WithTCLogger relies on TCTestOutputFilter to be present in the checkers.
            assertIs<TCTestOutputFilter>(checks.testFiltering.testOutputFilter)
            add(TestRunParameter.WithTCTestLogger)
            if (testName != null)
                add(TestRunParameter.WithTestFilter(testName))
            else {
//                    val ignoredTests = (testCase.extras as TestCase.WithTestRunnerExtras).ignoredTests
//                    if (ignoredTests.isNotEmpty()) {
//                        add(TestRunParameter.WithGTestPatterns(negativePatterns = ignoredTests))
//                    }
            }
        }
        TestKind.REGULAR -> {
            if (addTeamCityLogger) {
                // WithTCLogger relies on TCTestOutputFilter to be present in the checkers.
                assertIs<TCTestOutputFilter>(checks.testFiltering.testOutputFilter)
                add(TestRunParameter.WithTCTestLogger)
            }

            if (addTestFilter) {
                if (testName != null)
                    add(TestRunParameter.WithTestFilter(testName))
                else {
                    // TODO: questionable place for batch mode
                    val testRoots = testServices.testRunSettings.get<TestRoots>()
                    val nominalPackageName = computePackageName(
                        testDataBaseDir = testRoots.baseDir,
                        testDataFile = testServices.moduleStructure.originalTestDataFiles.first(),
                    )
                    TestRunParameter.WithPackageFilter(nominalPackageName)
                }
            }
        }
        else -> error("Not yet supported test kind: $testKind")
    }
}

class RunnerWithExecutorAndPrettyHandler(
    executor: Executor,
    testRun: TestRun,
    val testServices: TestServices,
    val exceptionWrapper: (Throwable) -> WrappedException.FromGroupingHandler,
) : RunnerWithExecutor(executor, testRun) {
    override fun buildResultHandler(runResult: RunResult): PrettyResultsHandler {
        return PrettyResultsHandler(
            runResult = runResult,
            checks = testRun.checks,
            testRun = testRun,
            loggedParameters = getLoggedParameters(),
            testServices,
            exceptionWrapper,
        )
    }
}

class PrettyResultsHandler(
    runResult: RunResult,
    checks: TestRunChecks,
    testRun: TestRun,
    loggedParameters: LoggedData.TestRunParameters,
    val testServices: TestServices,
    val exceptionWrapper: (Throwable) -> WrappedException.FromGroupingHandler,
) : ResultHandler(runResult, checks, testRun, loggedParameters) {
    companion object {
        @Suppress("RegExpRepeatedSpace")
        val failedRegexWithoutTCLogger = """\[  FAILED  ] (.*)\.__launcher__Kt.runTest""".toRegex()
        val failedRegexWithTCLogger = """-\s+(.*)\.__launcher__Kt.runTest""".toRegex()
    }

    override fun processNonExpectedFailure(failedResults: List<TestRunCheck.Result.Failed>) {
        val output = getLoggedRun().toString()
        val failedTests = if (testRun.runParameters.contains(TestRunParameter.WithTCTestLogger)) {
            failedResults.flatMap { findFailedTestsWithTCLogger(it.reason) }
        } else {
            findFailedTestsWithoutTCLogger(output)
        }
        val phaseInputs = testServices.groupingPhaseInputs

        if (phaseInputs.size == 1) {
            check(failedTests.size <= 1) {
                "There should be at most one failed test in the batch mode, but there were $failedTests"
            }
            if (failedTests.isNotEmpty()) {
                phaseInputs.single().catchingExecutor.executeWithCatching(exceptionWrapper) {
                    super.processNonExpectedFailure(failedResults)
                }
            }
            return
        }

        for (failedTest in failedTests) {
            val correspondingInput = phaseInputs.find {
                val testInfo = it.testInfo
                val correspondingTestName = BatchingPackageInserter.computePackage(testInfo)
                correspondingTestName == failedTest
            } ?: error("Can't find corresponding input for $failedTest")
            correspondingInput.catchingExecutor.executeWithCatching(exceptionWrapper) {
                super.processNonExpectedFailure(failedResults)
            }
        }

        if (failedResults.isNotEmpty() && failedTests.isEmpty()) {
            error("There should be at least one failed test in the batch mode, but there were none")
        }
    }

    private fun findFailedTestsWithoutTCLogger(output: String): List<String> {
        return failedRegexWithoutTCLogger.findAll(output)
            .map { it.groupValues }.distinct()
            .map { it[1] }.toList()
    }

    private fun findFailedTestsWithTCLogger(output: String): List<String> {
        return failedRegexWithTCLogger.findAll(output)
            .map { it.groupValues }.distinct()
            .map { it[1] }.toList()
    }
}
