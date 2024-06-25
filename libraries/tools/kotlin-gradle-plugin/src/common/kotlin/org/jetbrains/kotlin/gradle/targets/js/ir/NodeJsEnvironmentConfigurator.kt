/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

class NodeJsEnvironmentConfigurator(subTarget: KotlinJsIrSubTarget) :
    JsEnvironmentConfigurator<NodeJsExec>(subTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<NodeJsExec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            if (binary.mode == KotlinJsBinaryMode.DEVELOPMENT) "" else binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )

        val locateTask = project.locateTask<NodeJsExec>(binaryRunName)
        if (locateTask != null) return locateTask

        val compilation = binary.compilation
        return NodeJsExec.create(compilation, binaryRunName) {
            group = subTarget.taskGroupName
            dependsOn(project.tasks.named(subTarget.binarySyncTaskName(binary)))

            if (subTarget.target.wasmTargetType == KotlinWasmTargetType.WASI) {
                sourceMapStackTraces = false
            }

            val inputFile = project.objects.fileProperty().value(
                subTarget.binarySyncOutput(binary).flatMap {
                    it.file(binary.mainFileName)
                }
            )
            inputFileProperty.set(
                inputFile
            )
            runTaskConfigurations.all {
                it.execute(this)
            }
        }
    }
}