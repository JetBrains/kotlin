/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail

internal object TestRunners {
    // Currently, only local and noop test runners are supported.
    fun createProperTestRunner(testRun: TestRun, settings: Settings): Runner<Unit> = with(settings) {
        if (get<ForcedNoopTestRunner>().value) {
            NoopTestRunner
        } else with(get<KotlinNativeTargets>()) {
            if (testTarget == hostTarget)
                LocalTestRunner(testRun)
            else
                runningAtNonHostTarget()
        }
    }

    // Currently, only local test name extractor is supported.
    fun extractTestNames(executable: TestExecutable, settings: Settings): Collection<TestName> = with(settings) {
        with(get<KotlinNativeTargets>()) {
            if (testTarget == hostTarget)
                LocalTestNameExtractor(executable, TestRunChecks.Default(get<Timeouts>().executionTimeout)).run()
            else
                runningAtNonHostTarget()
        }
    }

    private fun KotlinNativeTargets.runningAtNonHostTarget(): Nothing = fail {
        """
            Running at non-host target is not supported yet.
            Compilation target: $testTarget
            Host target: $hostTarget
        """.trimIndent()
    }
}
