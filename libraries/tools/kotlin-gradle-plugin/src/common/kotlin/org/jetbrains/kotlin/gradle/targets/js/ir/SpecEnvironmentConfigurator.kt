/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.wasm.spec.SpecExec
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@ExperimentalWasmDsl
class SpecEnvironmentConfigurator(subTarget: KotlinJsIrSubTarget) :
    JsEnvironmentConfigurator<SpecExec>(subTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<SpecExec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )
        val locateTask = project.locateTask<SpecExec>(binaryRunName)
        if (locateTask != null) return locateTask

        return SpecExec.register(binary.compilation, binaryRunName) {
            group = subTarget.taskGroupName
            dependsOn(binary.linkSyncTask)
            runTaskConfigurations.all {
                it.execute(this)
            }
        }
    }
}