/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.Action
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.ExecutableWasm
import org.jetbrains.kotlin.gradle.targets.js.ir.JsEnvironmentConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.LibraryWasm
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@ExperimentalWasmDsl
internal class WasmtimeEnvironmentConfigurator(private val wasmtimeSubTarget: KotlinWasmtimeSubtarget) :
    JsEnvironmentConfigurator<WasmtimeExec>(wasmtimeSubTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<WasmtimeExec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            KotlinJsIrSubTarget.RUN_TASK_NAME
        )
        val locateTask = project.locateTask<WasmtimeExec>(binaryRunName)
        if (locateTask != null) return locateTask

        val compilation = binary.compilation
        return WasmtimeExec.register(compilation, binaryRunName) {
            group = subTarget.taskGroupName

            val inputWasmFile = when (binary) {
                is ExecutableWasm -> {
                    dependsOn(binary.componentTask)
                    binary.mainWasmFile
                }
                is LibraryWasm -> {
                    binary.mainWasmFile
                }
                else -> error("Binary for wasm can be either ExecutableWasm or LibraryWasm")
            }

            inputFileProperty.value(inputWasmFile)

            wasmtimeArgs.set(wasmtimeSubTarget.wasmtimeRunArgs)
        }
    }

    override fun configureRun(body: Action<WasmtimeExec>) {
        // do nothing
    }
}
