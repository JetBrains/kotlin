/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.NativeSimpleTestSupport
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.SimpleTestRunProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestRunSettings
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NativeSimpleTestSupport::class)
abstract class AbstractNativeSimpleTest {
    internal lateinit var testRunSettings: SimpleTestRunSettings
    internal lateinit var testRunProvider: SimpleTestRunProvider

    internal fun runExecutableAndVerify(testCase: TestCase, executable: TestExecutable) {
        val testRun = testRunProvider.getTestRun(testCase, executable)
        val testRunner = createProperTestRunner(testRun, testRunSettings)
        testRunner.run()
    }
}
