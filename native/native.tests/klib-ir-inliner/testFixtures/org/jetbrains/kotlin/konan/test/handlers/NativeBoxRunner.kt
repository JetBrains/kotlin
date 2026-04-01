/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.parseTestKind
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
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.groupingPhaseInputs
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.model.TestModule
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
            addTeamCityLogger = false,
            addTestFilter = false,
        )
        val testRunner = createProperTestRunner(testRun, testServices.testRunSettings) { executor, testRun ->
            RunnerWithExecutorAndPrettyHandler(executor, testRun, testServices)
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
    val testKind = parseTestKind(testServices.moduleStructure.modules.firstOrNull()?.directives) ?: testServices.testRunSettings.get<TestKind>()
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
) : RunnerWithExecutor(executor, testRun) {
    override fun buildResultHandler(runResult: RunResult): PrettyResultsHandler {
        return PrettyResultsHandler(
            runResult = runResult,
            checks = testRun.checks,
            testRun = testRun,
            loggedParameters = getLoggedParameters(),
            testServices
        )
    }
}

class PrettyResultsHandler(
    runResult: RunResult,
    checks: TestRunChecks,
    testRun: TestRun,
    loggedParameters: LoggedData.TestRunParameters,
    val testServices: TestServices,
) : ResultHandler(runResult, checks, testRun, loggedParameters) {
    companion object {
        @Suppress("RegExpRepeatedSpace")
        val failedRegex = """\[  FAILED  ] (.*)\.(.*)\.__launcher__Kt.runTest""".toRegex()
    }

    override fun processNonExpectedFailure(failedResults: List<TestRunCheck.Result.Failed>) {
        val output = getLoggedRun().toString()
        val failedTests = failedRegex.findAll(output)
            .map { it.groupValues }
            .distinct()
            .map { it[1] to it[2] }
            .toList()
        val phaseInputs = testServices.groupingPhaseInputs
        for ((className, methodName) in failedTests) {
            val correspondingInput = phaseInputs.find {
                val testInfo = it.testInfo
                testInfo.className.replace("$", ".").endsWith(className) && testInfo.methodName == methodName
            } ?: error("Can't find corresponding input for $className.$methodName")
            correspondingInput.catchingExecutor.executeWithCatching {
                super.processNonExpectedFailure(failedResults)
            }
        }
    }
}
