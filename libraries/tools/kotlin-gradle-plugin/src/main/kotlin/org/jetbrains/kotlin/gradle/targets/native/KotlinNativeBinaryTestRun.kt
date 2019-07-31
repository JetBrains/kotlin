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

open class DefaultKotlinNativeTestRun(testRunName: String, target: KotlinNativeTarget) :
    KotlinTaskTestRun<NativeBinaryTestRunSource, KotlinNativeTest>(testRunName, target),
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
            "Expected a test fbinary of target ${target.name}, " +
                    "got the binary ${testExecutable.name} of target ${testExecutable.target.name}"
        }

        executionSource = NativeBinaryTestRunSource(testExecutable)
    }
}