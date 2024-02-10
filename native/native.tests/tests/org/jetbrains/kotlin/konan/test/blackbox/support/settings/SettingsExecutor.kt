/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.ConfigurablesWithEmulator
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.native.executors.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import java.util.concurrent.ConcurrentHashMap

private val executorCache: ConcurrentHashMap<KonanTarget, Executor> = ConcurrentHashMap()

internal val Settings.executor: Executor
    get() = with(get<KotlinNativeTargets>()) {
        executorCache.computeIfAbsent(testTarget) {
            val configurables = configurables
            when {
                configurables.target == hostTarget -> HostExecutor()
                configurables is ConfigurablesWithEmulator -> EmulatorExecutor(configurables)
                configurables is AppleConfigurables && configurables.targetTriple.isSimulator ->
                    XcodeSimulatorExecutor(configurables)
                configurables is AppleConfigurables && RosettaExecutor.availableFor(configurables) -> RosettaExecutor(configurables)
                else -> JUnit5Assertions.fail { "Running tests for $testTarget on $hostTarget is not supported yet." }
            }
        }
    }