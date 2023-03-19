/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.native.executors.Executor
import org.jetbrains.kotlin.native.executors.EmulatorExecutor
import org.jetbrains.kotlin.native.executors.XcodeSimulatorExecutor
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.util.concurrent.ConcurrentHashMap

internal object TestRunners {
    fun createProperTestRunner(testRun: TestRun, settings: Settings): Runner<Unit> = with(settings) {
        if (get<ForcedNoopTestRunner>().value) {
            NoopTestRunner
        } else with(get<KotlinNativeTargets>()) {
            if (testTarget == hostTarget) {
                LocalTestRunner(testRun)
            } else {
                val nativeHome = get<KotlinNativeHome>()
                val distribution = Distribution(nativeHome.dir.path)
                val configurables = PlatformManager(distribution, true).platform(testTarget).configurables

                val executor = cached(
                    when {
                        configurables is ConfigurablesWithEmulator -> EmulatorExecutor(configurables)
                        configurables is AppleConfigurables && configurables.targetTriple.isSimulator ->
                            XcodeSimulatorExecutor(configurables)
                        else -> runningOnUnsupportedTarget()
                    }
                )
                RunnerWithExecutor(executor, testRun)
            }
        }
    }

    private val runnersCache: ConcurrentHashMap<String, Executor> = ConcurrentHashMap()

    private inline fun <reified T : Executor> cached(executor: T): Executor =
        runnersCache.computeIfAbsent(T::class.java.simpleName) { executor }

    // Currently, only local test name extractor is supported.
    fun extractTestNames(executable: TestExecutable, settings: Settings): Collection<TestName> = with(settings) {
        with(get<KotlinNativeTargets>()) {
            if (testTarget == hostTarget)
                LocalTestNameExtractor(executable, TestRunChecks.Default(get<Timeouts>().executionTimeout)).run()
            else
                runningOnUnsupportedTarget()
        }
    }

    private fun KotlinNativeTargets.runningOnUnsupportedTarget(): Nothing = fail {
        "Running tests for $testTarget on $hostTarget is not supported yet."
    }
}
