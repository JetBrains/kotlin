/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmtimeDsl
import org.jetbrains.kotlin.gradle.targets.wasm.testing.KotlinWasmtime
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimePlugin
import javax.inject.Inject

@OptIn(ExperimentalWasmDsl::class)
internal abstract class KotlinWasmtimeSubtarget
@Inject
internal constructor(
    target: KotlinJsIrTarget,
) :
    KotlinJsIrSubTarget(target, "wasmtime"),
    KotlinWasmtimeDsl {

    private val wasmtime = WasmtimePlugin.applyWithEnvSpec(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside Wasmtime"

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        test.testFramework = KotlinWasmtime(test)
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        with(wasmtime) {
            test.dependsOn(project.wasmtimeSetupTaskProvider)
        }
    }

    override fun configureTestInputFile(
        test: KotlinJsTest,
        binary: JsIrBinary,
    ) {
        binary as WasmBinary
        test.inputFileProperty.set(binary.mainWasmFile)
    }
}
