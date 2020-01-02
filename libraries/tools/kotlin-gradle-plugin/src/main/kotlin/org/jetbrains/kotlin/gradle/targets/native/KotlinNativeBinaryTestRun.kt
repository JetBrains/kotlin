/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.jetbrains.kotlin.gradle.plugin.CompilationExecutionSource
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetTestRun
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun

class NativeBinaryTestRunSource(val binary: TestExecutable) :
    CompilationExecutionSource<KotlinNativeCompilation> {

    override val compilation: KotlinNativeCompilation
        get() = binary.compilation
}

interface KotlinNativeBinaryTestRun : KotlinTargetTestRun<NativeBinaryTestRunSource> {
    /**
     * Sets this test run to use the specified [testExecutable].
     *
     * This overrides other [executionSource] options.
     */
    fun setExecutionSourceFrom(testExecutable: TestExecutable)
}

interface KotlinNativeHostTestRun : KotlinNativeBinaryTestRun

interface KotlinNativeSimulatorTestRun : KotlinNativeBinaryTestRun {
    /**
     * Determines which simulated device will be used to execute the test run.
     * To get list of all available devices, run `xcrun simctl list`.
     */
    var deviceId: String
}

abstract class AbstractKotlinNativeTestRun<T : KotlinNativeTest>(testRunName: String, target: KotlinNativeTarget) :
    KotlinTaskTestRun<NativeBinaryTestRunSource, T>(testRunName, target),
    KotlinNativeBinaryTestRun {

    private lateinit var _executionSource: NativeBinaryTestRunSource

    final override var executionSource: NativeBinaryTestRunSource
        get() = _executionSource
        private set(value) {
            executionTask.configure {
                it.executable(value.binary.linkTask) { value.binary.outputFile }
            }
            _executionSource = value
        }

    override fun setExecutionSourceFrom(testExecutable: TestExecutable) {
        require(testExecutable.target === target) {
            "Expected a test binary of target ${target.name}, " +
                    "got the binary ${testExecutable.name} of target ${testExecutable.target.name}"
        }

        executionSource = NativeBinaryTestRunSource(testExecutable)
    }
}

open class DefaultHostTestRun(testRunName: String, target: KotlinNativeTarget) :
    AbstractKotlinNativeTestRun<KotlinNativeHostTest>(testRunName, target),
    KotlinNativeHostTestRun

open class DefaultSimulatorTestRun(testRunName: String, target: KotlinNativeTarget) :
    AbstractKotlinNativeTestRun<KotlinNativeSimulatorTest>(testRunName, target),
    KotlinNativeSimulatorTestRun {

    override var deviceId: String
        get() = executionTask.get().deviceId
        set(value) {
            executionTask.get().deviceId = value
        }
}