/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.SharedExecutionTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.executor
import org.jetbrains.kotlin.native.executors.Executor

internal object TestRunners {
    fun createProperTestRunner(testRun: TestRun, settings: Settings): Runner<Unit> = with(settings) {
        if (get<ForcedNoopTestRunner>().value) {
            NoopTestRunner
        } else {
            executor.toRunner(settings, testRun)
        }
    }

    private fun Executor.toRunner(settings: Settings, testRun: TestRun): AbstractRunner<Unit> =
        if (settings.get<SharedExecutionTestRunner>().value) {
            SharedExecutionBuilder.buildRunner(settings, this, testRun)
        } else {
            RunnerWithExecutor(this, testRun)
        }
}
