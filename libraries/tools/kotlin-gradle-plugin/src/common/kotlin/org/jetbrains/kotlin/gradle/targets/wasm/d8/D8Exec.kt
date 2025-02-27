/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask

@ExperimentalWasmDsl
@DisableCachingByDefault
@Suppress("DEPRECATION_ERROR")
abstract class D8Exec internal constructor() : org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec() {
    companion object {
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: D8Exec.() -> Unit = {},
        ): TaskProvider<D8Exec> {
            val target = compilation.target
            val project = target.project
            val d8 = D8Plugin.applyWithEnvSpec(project)
            return project.registerTask(
                name
            ) {
                it.executable = d8.executable.get()
                with(d8) {
                    it.dependsOn(project.d8SetupTaskProvider)
                }
                it.dependsOn(compilation.compileTaskProvider)
                it.configuration()
            }
        }

        @Deprecated("Use register instead", ReplaceWith("register(compilation, name, configuration)"))
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: D8Exec.() -> Unit = {},
        ): TaskProvider<D8Exec> = register(compilation, name, configuration)
    }
}