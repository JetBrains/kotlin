/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.d8.toolchain.isD8ToolchainDisabled
import org.jetbrains.kotlin.gradle.targets.wasm.d8.toolchain.requestDefaultD8
import org.jetbrains.kotlin.gradle.targets.wasm.d8.toolchain.resolveD8Executable
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesD8ToolchainService
import org.jetbrains.kotlin.gradle.utils.newFileProperty

@OptIn(ExperimentalKotlinToolchainDsl::class)
@ExperimentalWasmDsl
@DisableCachingByDefault
@Suppress("DEPRECATION_ERROR")
abstract class D8Exec internal constructor() : AbstractExecTask<D8Exec>(D8Exec::class.java), UsesD8ToolchainService {
    init {
        this.onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @get:Input
    abstract val d8Args: ListProperty<String>

    /**
     * The D8 executable of the old, environment spec based setup. It is empty when the D8 toolchain provisions D8.
     */
    @get:Input
    @get:Optional
    internal abstract val legacyExecutable: Property<String>

    @get:Nested
    internal val d8Request: Provider<ToolchainRequest> = project.requestDefaultD8()

    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    override fun exec() {
        // Resolved at execution time, because it may provision D8
        executable = d8ToolchainService.get().resolveD8Executable(legacyExecutable, d8Request)

        val newArgs = mutableListOf<String>()
        newArgs.addAll(d8Args.get())
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
            val toolchainDisabled = project.isD8ToolchainDisabled()
            return project.registerTask(
                name
            ) {
                if (toolchainDisabled) {
                    it.legacyExecutable.set(d8.executable)
                    with(d8) {
                        it.dependsOn(project.d8SetupTaskProvider)
                    }
                }
                it.dependsOn(compilation.compileTaskProvider)
                it.d8Args.convention(listOf("--experimental-wasm-wasmfx"))
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
