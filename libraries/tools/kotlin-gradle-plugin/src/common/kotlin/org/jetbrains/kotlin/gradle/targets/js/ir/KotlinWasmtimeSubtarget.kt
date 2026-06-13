/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmtimeDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmtime
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimePlugin
import org.jetbrains.kotlin.gradle.utils.withType
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

    override fun runTask(body: Action<WasmtimeExec>) {
        subTargetConfigurators
            .withType<WasmtimeEnvironmentConfigurator>()
            .configureEach {
                it.configureRun(body)
            }
    }

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
        test.dependsOn(binary.linkTask)
        test.inputFileProperty.fileProvider(binary.mainWasmFile)
    }
}
