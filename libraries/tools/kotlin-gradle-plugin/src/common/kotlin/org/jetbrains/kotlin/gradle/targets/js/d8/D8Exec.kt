/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newFileProperty

@ExperimentalWasmDsl
@DisableCachingByDefault
open class D8Exec : AbstractExecTask<D8Exec>(D8Exec::class.java) {
    init {
        this.onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @Input
    var d8Args: MutableList<String> = mutableListOf()

    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    override fun exec() {
        val newArgs = mutableListOf<String>()
        newArgs.addAll(d8Args)
        if (inputFileProperty.isPresent) {
            val inputFile = inputFileProperty.asFile.get()
            workingDir = inputFile.parentFile
            newArgs.add("--module")
            newArgs.add(inputFile.absolutePath)
        }
        args?.let {
            if (it.isNotEmpty()) {
                newArgs.add("--")
                newArgs.addAll(it)
            }
        }
        this.args = newArgs
        super.exec()
    }

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

        @Deprecated(
            "Use register instead. Scheduled for removal in Kotlin 2.3.",
            ReplaceWith("register(compilation, name, configuration)"),
            level = DeprecationLevel.ERROR
        )
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: D8Exec.() -> Unit = {},
        ): TaskProvider<D8Exec> = register(compilation, name, configuration)
    }
}