/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import javax.inject.Inject

/**
 * Produces a WebAssembly component out of a core Wasm module using `wasm-tools`.
 *
 * The task runs two commands sequentially:
 * - `wasm-tools component embed <witDirectory> <inputFile> -o <embeddedFile>`
 *   embeds the WIT declarations into the core module,
 *   all directories of [witDirectory] are merged into a single one beforehand
 * - `wasm-tools component new <embeddedFile> -o <componentFile>`
 *   converts the core module with embedded declarations into a component
 *
 * `wasm-tools` is expected to be available in `PATH`,
 * otherwise [executable] must point to the `wasm-tools` binary.
 *
 */
@ExperimentalWasmDsl
@DisableCachingByDefault
abstract class WasmComponentExec
internal constructor() : DefaultTask() {
    @get:Inject
    internal abstract val execOperations: ExecOperations

    @get:Inject
    internal abstract val fs: FileSystemOperations

    /**
     * `wasm-tools` executable, `wasm-tools` from `PATH` by default.
     */
    @get:Input
    abstract val executable: Property<String>

    /**
     * Core Wasm module to be turned into a component.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    abstract val inputFile: RegularFileProperty

    /**
     * Directories with all WIT declarations of the component.
     *
     * Contents of all these directories are merged into a single directory,
     * which is then passed to the `wasm-tools component embed` command.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val witDirectory: ConfigurableFileCollection

    /**
     * Additional arguments of the `wasm-tools component embed` command.
     */
    @get:Input
    abstract val embedArguments: ListProperty<String>

    /**
     * Additional arguments of the `wasm-tools component new` command.
     */
    @get:Input
    abstract val newArguments: ListProperty<String>

    /**
     * Resulting Wasm component.
     */
    @get:OutputFile
    abstract val componentFile: RegularFileProperty

    @TaskAction
    fun run() {
        val wasmTools = executable.get()
        val component = componentFile.getFile()

        component.parentFile.mkdirs()

        val embedded = temporaryDir.resolve(inputFile.getFile().name)

        val witDir = temporaryDir.resolve(WIT_DIRECTORY_NAME)

        fs.delete {
            it.delete(witDir)
        }

        fs.copy {
            it.from(witDirectory)
            it.into(witDir)
        }

        execOperations.exec {
            it.executable = wasmTools
            it.args = listOf(
                "component",
                "embed",
                witDir.absolutePath,
                inputFile.getFile().absolutePath
            ) +
                    embedArguments.get() +
                    listOf("-o", embedded.absolutePath)
        }

        val adaptFile = temporaryDir
            .resolve("wasi_snapshot_preview1.reactor.wasm")
            .also {
                it.outputStream()
                    .use { tmpDir ->
                        WasmComponentExec::class.java
                            .getResourceAsStream("/org/jetbrains/kotlin/gradle/targets/wasm/component/wasi_snapshot_preview1.reactor.wasm")
                            ?.copyTo(tmpDir)
                    }
            }

        component.parentFile.mkdirs()
        execOperations.exec {
            it.executable = wasmTools
            it.args = listOf(
                "component",
                "new",
                embedded.absolutePath,
                "--adapt",
                adaptFile.absolutePath
            ) +
                    newArguments.get() +
                    listOf("-o", component.absolutePath)
        }
    }

    companion object {
        /**
         * Default `wasm-tools` executable, resolved from `PATH`.
         */
        const val WASM_TOOLS_EXECUTABLE = "wasm-tools"

        internal const val WIT_DIRECTORY_NAME = "wit"

        @ExperimentalWasmDsl
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: WasmComponentExec.() -> Unit = {},
        ): TaskProvider<WasmComponentExec> {
            val project = compilation.target.project
            return project.registerTask(
                name,
            ) {
                it.executable.convention(WASM_TOOLS_EXECUTABLE)
                it.witDirectory.convention(project.layout.buildDirectory.dir(WIT_DIRECTORY_NAME))
                it.configuration()
            }
        }
    }
}
