/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.LocalTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRun
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks

@Tag("cinterop")
abstract class AbstractNativeInteropIndexerFModulesTest : AbstractNativeInteropIndexerTest() {
    override val fmodules = true
}

@Tag("cinterop")
abstract class AbstractNativeInteropIndexerNoFModulesTest : AbstractNativeInteropIndexerTest() {
    override val fmodules = false
}

@Tag("cinterop")
abstract class AbstractNativeInteropIndexerTest : AbstractNativeSimpleTest() {
    abstract val fmodules: Boolean

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = File(KtTestUtil.getHomeDirectory()).resolve(testPath)
        val tempDir = testRunSettings.get<BaseDirs>().testBuildDir.resolve(testPath).apply { mkdirs() }

        val dummyCompilerCall = LoggedData.CompilerCall(
            parameters = LoggedData.CompilerParameters(home = testRunSettings.get(), compilerArgs = arrayOf(), sourceModules = listOf()),
            exitCode = ExitCode.OK,
            compilerOutput = "",
            compilerOutputHasErrors = false,
            duration = 0.seconds
        )
        val testRun = TestRun(
            executable = TestExecutable(
                executableFile = testPathFull.parentFile.parentFile.resolve("cinterop_contents.sh").canonicalFile,
                testNames = listOf(),
                loggedCompilerCall = dummyCompilerCall
            ),
            displayName = testPath,
            testCaseId = TestCaseId.Named(testPath),
            runParameters = listOf(
                TestRunParameter.WithFreeCommandLineArguments(
                    listOf(
                        testPathFull.canonicalPath,
                        tempDir.canonicalPath,
                        if (fmodules) "-compiler-option -fmodules" else ""
                    )
                )
            ),
            checks = TestRunChecks(
                executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(timeout = 30.seconds),
                exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
                outputDataFile = TestRunCheck.OutputDataFile(testPathFull.resolve("contents.gold.txt"))
            )
        )
        LocalTestRunner(testRun).run()
    }
}
