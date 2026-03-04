/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.SharedExecutionTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.testProcessExecutor
import org.jetbrains.kotlin.native.executors.Executor

object TestRunners {
    fun createProperTestRunner(
        testRun: TestRun,
        settings: Settings,
        runnerWithExecutorProducer: (Executor, TestRun) -> Runner<Unit> = ::RunnerWithExecutor,
    ): Runner<Unit> = with(settings) {
        when {
            get<ForcedNoopTestRunner>().value -> NoopTestRunner
            settings.get<SharedExecutionTestRunner>().value -> SharedExecutionBuilder.buildRunner(settings, testProcessExecutor, testRun)
            else -> runnerWithExecutorProducer(testProcessExecutor, testRun)
        }
    }
}
