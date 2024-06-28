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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.extension.ExtendWith
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
        val absoluteTestFile = getAbsoluteFile(testDataFilePath)
        val testCaseId = TestCaseId.TestDataFile(absoluteTestFile)

        val isMuted = testRunSettings.isIgnoredTarget(absoluteTestFile)
        try {
            testRunProvider.getSingleTestRun(testCaseId, testRunSettings)
        } catch (e: CompilationToolException) {
            if (isMuted) {
                println("There was an expected failure: CompilationToolException: ${e.reason}")
                return
            } else {
                fail { e.reason }
            }
        }
        if (isMuted) {
            fail {
                "Looks like this test can be unmuted."
            }
        }
    }

    final override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = null
}