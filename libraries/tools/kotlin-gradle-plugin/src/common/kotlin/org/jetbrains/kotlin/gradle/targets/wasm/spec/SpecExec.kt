/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.spec

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask

@ExperimentalWasmDsl
@DisableCachingByDefault
@Suppress("DEPRECATION_ERROR")
abstract class SpecExec internal constructor() : AbstractExecTask<SpecExec>(SpecExec::class.java) {
    companion object {
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: SpecExec.() -> Unit = {},
        ): TaskProvider<SpecExec> {
            val target = compilation.target
            val project = target.project
            val spec = SpecPlugin.applyWithSpecEnv(project)
            return project.registerTask(
                name
            ) {
                it.executable = spec.executable.get()
                it.dependsOn(compilation.compileTaskProvider)
                it.configuration()
            }
        }
    }
}