/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.withType

abstract class JsEnvironmentConfigurator<RunTask : Task>(protected val subTarget: KotlinJsIrSubTarget) :
    SubTargetConfigurator<Copy, RunTask> {
    protected val project = subTarget.target.project

    protected val runTaskConfigurations = project.objects.domainObjectSet<Action<RunTask>>()

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .getIrBinaries(KotlinJsBinaryMode.PRODUCTION)
            .withType<Executable>()
            .configureEach{ productionExecutable ->
                val assembleTask = if (subTarget.target.wasmTargetType == KotlinWasmTargetType.WASI) {
                    (productionExecutable as WasmBinary).optimizeTask
                } else {
                    project.tasks.named(subTarget.binarySyncTaskName(productionExecutable))
                }

                if (compilation.isMain()) {
                    project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(
                        assembleTask
                    )
                }
            }
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .withType<Executable>()
            .configureEach { developmentExecutable ->
                configureBinaryRun(developmentExecutable)
            }
    }

    override fun configureRun(body: Action<RunTask>) {
        runTaskConfigurations.add(body)
    }

    override fun configureBuild(body: Action<Copy>) {
        // do nothing
    }

    protected abstract fun configureBinaryRun(binary: JsIrBinary): TaskProvider<RunTask>
}