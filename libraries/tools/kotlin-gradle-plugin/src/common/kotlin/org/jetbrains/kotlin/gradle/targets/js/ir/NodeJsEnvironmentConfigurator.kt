/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@OptIn(ExperimentalWasmDsl::class)
class NodeJsEnvironmentConfigurator(subTarget: KotlinJsIrSubTarget) :
    JsEnvironmentConfigurator<NodeJsExec>(subTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<NodeJsExec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )

        val locateTask = project.locateTask<NodeJsExec>(binaryRunName)
        if (locateTask != null) return locateTask

        val compilation = binary.compilation
        return NodeJsExec.register(compilation, binaryRunName) {
            group = subTarget.taskGroupName

            if (subTarget.target.wasmTargetType == KotlinWasmTargetType.WASI) {
                sourceMapStackTraces = false
            }

            val inputFile = if (compilation.target.wasmTargetType == KotlinWasmTargetType.WASI) {
                if (binary is ExecutableWasm && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                    dependsOn(binary.optimizeTask)
                    binary.mainOptimizedFile
                } else {
                    dependsOn(binary.linkTask)
                    binary.mainFile
                }
            } else {
                dependsOn(binary.linkSyncTask)
                binary.mainFileSyncPath
            }

            inputFileProperty.set(
                inputFile
            )
            runTaskConfigurations.all {
                it.execute(this)
            }
        }
    }
}