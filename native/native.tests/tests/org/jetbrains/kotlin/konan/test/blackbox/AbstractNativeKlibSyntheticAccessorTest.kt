/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.KlibSyntheticAccessorTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.backend.handlers.SyntheticAccessorsDumpHandler
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotNull
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.ExtendWith
import org.opentest4j.AssertionFailedError
import java.io.File

// TODO(KT-64570): Migrate these tests to the Compiler Core test infrastructure as soon as we move IR inlining
//   to the first compilation stage.
@ExtendWith(KlibSyntheticAccessorTestSupport::class)
abstract class AbstractNativeKlibSyntheticAccessorTest : ExternalSourceTransformersProvider {
    lateinit var testRunSettings: TestRunSettings
    lateinit var testRunProvider: TestRunProvider

    /**
     * Run JUnit test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.Test].
     */
    protected fun runTest(@TestDataFile testDataFilePath: String) {
        // In one-stage test mode of K/N, an intermediate klib in introduced between stages with unpredictable unique name,
        // and this messes the per-module logic in DumpSyntheticAccessors.
        // Synthetic accessors are not dependent on test mode, so safely may be tested only in TWO_STAGE_MULTI_MODULE
        Assumptions.assumeTrue(testRunSettings.get<TestMode>() == TestMode.TWO_STAGE_MULTI_MODULE)

        val absoluteTestFile = getAbsoluteFile(testDataFilePath)
        val testCaseId = TestCaseId.TestDataFile(absoluteTestFile)

        val isMuted = testRunSettings.isIgnoredTarget(absoluteTestFile)

        val testRunOrFailure = runCatching { testRunProvider.getSingleTestRun(testCaseId, testRunSettings) }
        testRunOrFailure.exceptionOrNull()?.let { exception ->
            when {
                exception !is CompilationToolException -> throw exception
                isMuted -> {
                    println("There was an expected failure: CompilationToolException: ${exception.reason}")
                    return
                }
                else -> fail { exception.reason }
            }
        }

        val testRun = testRunOrFailure.getOrThrow()

        val syntheticAccessorsDumpDir = testRun.executable.executable.syntheticAccessorsDumpDir
        assertNotNull(syntheticAccessorsDumpDir) { "No synthetic accessors dump directory" }
        assertTrue(syntheticAccessorsDumpDir!!.isDirectory) {
            "The synthetic accessors dump directory does not exist: $syntheticAccessorsDumpDir"
        }

        runCatching {
            with(SyntheticAccessorsDumpHandler) {
                JUnit5Assertions.assertSyntheticAccessorDumpIsCorrect(
                    dumpDir = syntheticAccessorsDumpDir,
                    moduleNames = testRun.testCase.modules.mapToSet { Name.identifier(it.name) },
                    testDataFile = absoluteTestFile
                )
            }
        }.exceptionOrNull()?.let { exception ->
            if (!isMuted || exception !is AssertionFailedError)
                throw exception
            println("There was an expected failure on synthetic accessors dump comparison: ${exception.message}")
            return
        }

        if (isMuted) {
            fail { "Looks like this test can be unmuted." }
        }
    }

    final override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = null
}