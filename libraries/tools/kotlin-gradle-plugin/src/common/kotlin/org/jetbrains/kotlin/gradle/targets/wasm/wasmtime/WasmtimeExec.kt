/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime

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
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.toolchain.isWasmtimeToolchainDisabled
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.toolchain.requestDefaultWasmtime
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.toolchain.resolveWasmtimeExecutable
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesWasmtimeToolchainService
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newFileProperty

@OptIn(ExperimentalKotlinToolchainDsl::class)
@ExperimentalWasmDsl
@DisableCachingByDefault
abstract class WasmtimeExec internal constructor() :
    AbstractExecTask<WasmtimeExec>(WasmtimeExec::class.java),
    UsesWasmtimeToolchainService {
    init {
        this.onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @get:Input
    abstract val wasmtimeArgs: ListProperty<String>

    /**
     * The Wasmtime executable of the old, environment spec based setup.
     * It is empty when the Wasmtime toolchain provisions Wasmtime.
     */
    @get:Input
    @get:Optional
    internal abstract val legacyExecutable: Property<String>

    @get:Nested
    internal val wasmtimeRequest: Provider<ToolchainRequest> = project.requestDefaultWasmtime()

    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    override fun exec() {
        // Resolved at execution time, because it may provision Wasmtime
        executable = wasmtimeToolchainService.get().resolveWasmtimeExecutable(legacyExecutable, wasmtimeRequest)

        val newArgs = mutableListOf<String>()
        newArgs.addAll(wasmtimeArgs.get())
        val inputFile = inputFileProperty.getFile()
        workingDir = inputFile.parentFile
        newArgs.add(inputFile.absolutePath)
        args?.let {
            if (it.isNotEmpty()) {
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
            configuration: WasmtimeExec.() -> Unit = {},
        ): TaskProvider<WasmtimeExec> {
            val target = compilation.target
            val project = target.project
            val wasmtime = WasmtimePlugin.applyWithEnvSpec(project)
            val toolchainDisabled = project.isWasmtimeToolchainDisabled()
            return project.registerTask(
                name
            ) {
                if (toolchainDisabled) {
                    it.legacyExecutable.set(wasmtime.executable)
                    with(wasmtime) {
                        it.dependsOn(project.wasmtimeSetupTaskProvider)
                    }
                }
                it.configuration()
            }
        }
    }
}
