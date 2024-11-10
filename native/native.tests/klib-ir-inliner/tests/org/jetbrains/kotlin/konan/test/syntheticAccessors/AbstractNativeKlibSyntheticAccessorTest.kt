/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.syntheticAccessors

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.support.KlibSyntheticAccessorTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.SyntheticAccessorsDumpHandler
import org.jetbrains.kotlin.test.directives.KlibIrInlinerTestDirectives.IGNORE_SYNTHETIC_ACCESSORS_CHECKS
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotNull
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.extension.ExtendWith
import org.opentest4j.AssertionFailedError
import java.io.File
import kotlin.test.assertEquals

// TODO(KT-64570): Migrate these tests to the Compiler Core test infrastructure as soon as we move IR inlining
//   to the first compilation stage.
// Then, check for `IGNORE_SYNTHETIC_ACCESSORS_CHECKS` should be replaced with configuration like in AbstractFirJsKlibSyntheticAccessorTest:
//         useAfterAnalysisCheckers(
//            ::BlackBoxCodegenSuppressor.bind(KlibIrInlinerTestDirectives.IGNORE_SYNTHETIC_ACCESSORS_CHECKS)
//        )
@ExtendWith(KlibSyntheticAccessorTestSupport::class)
abstract class AbstractNativeKlibSyntheticAccessorTest(
    internal val narrowedAccessorVisibility: Boolean
) : ExternalSourceTransformersProvider {
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
        // This testsuite isn't run in full K/Native test matrix, so let's be sure every test configuration has TWO_STAGE_MULTI_MODULE
        assertEquals(TestMode.TWO_STAGE_MULTI_MODULE, testRunSettings.get<TestMode>())

        val absoluteTestFile = getAbsoluteFile(testDataFilePath)
        val testCaseId = TestCaseId.TestDataFile(absoluteTestFile)

        val isMuted = InTextDirectivesUtils.isIgnoredTarget(TargetBackend.NATIVE, absoluteTestFile, true, "$IGNORE_SYNTHETIC_ACCESSORS_CHECKS:")

        val testRunOrFailure = runCatching { testRunProvider.getSingleTestRun(testCaseId, testRunSettings) }
        testRunOrFailure.exceptionOrNull()?.let { exception ->
            when {
                exception !is CompilationToolException -> throw exception
                isMuted -> {
                    return // Expected failure
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
                    testDataFile = absoluteTestFile,
                    withNarrowedVisibility = narrowedAccessorVisibility
                )
            }
        }.exceptionOrNull()?.let { exception ->
            if (!isMuted || exception !is AssertionFailedError)
                throw exception
            return // There was an expected failure on synthetic accessors dump comparison: ${exception.message}"
        }

        if (isMuted) {
            fail {
                "Test passed unexpectedly: $testDataFilePath. Please remove ${TargetBackend.NATIVE.name} from values of test directive `${IGNORE_SYNTHETIC_ACCESSORS_CHECKS.name}`"
            }
        }
    }

    final override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = null
}

open class AbstractNativeKlibSyntheticAccessorInPhase1Test : AbstractNativeKlibSyntheticAccessorTest(narrowedAccessorVisibility = true)
open class AbstractNativeKlibSyntheticAccessorInPhase2Test : AbstractNativeKlibSyntheticAccessorTest(narrowedAccessorVisibility = false)
