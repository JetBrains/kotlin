/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.binaryenArgs
import javax.inject.Inject

@DisableCachingByDefault
abstract class BinaryenExec
@Inject
constructor() : AbstractExecTask<BinaryenExec>(BinaryenExec::class.java) {
    @get:Inject
    abstract val fs: FileSystemOperations

    init {
        onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map { it.exists() }.get()
        }
    }

    @get:Input
    internal var perModule: Boolean = false

    @Internal
    @Deprecated("Use binaryenArguments instead. Scheduled for removal in Kotlin 2.5.")
    var binaryenArgs: MutableList<String> = binaryenArgs(perModule).toMutableList()

    @Suppress("DEPRECATION")
    @get:Input
    val binaryenArguments: ListProperty<String> = project.objects.listProperty(String::class.java).convention(binaryenArgs)

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val outputFileName: Property<String>

    @Internal
    val outputFileProperty: Provider<RegularFile> = project.provider {
        outputDirectory.file(outputFileName).get()
    }

    override fun exec() {
        val inputFile = inputFileProperty.asFile.get()
        val newArgs = mutableListOf<String>()
        newArgs.addAll(binaryenArguments.get())
        newArgs.add(inputFile.absolutePath)
        newArgs.add("-o")
        newArgs.add(outputDirectory.file(outputFileName).get().asFile.absolutePath)
        workingDir = inputFile.parentFile
        this.args = newArgs
        super.exec()
    }

    companion object {
        @ExperimentalWasmDsl
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: BinaryenExec.() -> Unit = {},
        ): TaskProvider<BinaryenExec> {
            val target = compilation.target
            val project = target.project
            val binaryen = BinaryenPlugin.apply(project)
            return project.registerTask(
                name,
            ) {
                it.executable = binaryen.requireConfigured().executable
                it.dependsOn(binaryen.setupTaskProvider)
                it.dependsOn(compilation.compileTaskProvider)
                if (project.kotlinPropertiesProvider.wasmPerModule && compilation.wasmTarget != WasmTarget.WASI) {
                    it.perModule = true
                }
                it.configuration()
            }
        }

        @ExperimentalWasmDsl
        @Deprecated(
            "Use register instead",
            ReplaceWith("register(compilation, name, configuration)")
        )
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: BinaryenExec.() -> Unit = {},
        ): TaskProvider<BinaryenExec> = register(compilation, name, configuration)
    }
}