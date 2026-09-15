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
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import java.io.File
import javax.inject.Inject

/**
 * Produces a WebAssembly component out of a core Wasm module using `wasm-tools`.
 *
 * Every WIT project of [witDirectory] is embedded into the module by its own
 * `wasm-tools component embed` run, each run taking the result of the previous one as its input,
 * and the last result is converted into a component by a single `wasm-tools component new` run:
 *
 * ```
 * inputFile --embed(witDirectory[0])--> ... --embed(witDirectory[n])--> new --> componentFile
 * ```
 *
 * Every WIT project is resolved independently, so it must be self-contained:
 * one root package declaring a world plus all packages it references under `deps`.
 *
 * Declarations of all runs are merged by `wasm-tools component new` into a single world of the component,
 * so the module has to export everything that all these worlds export together,
 * while imports which the module does not use are dropped.
 *
 * Intermediate modules are stored in the temporary directory of the task.
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
     * WIT projects to be embedded into the component.
     *
     * Every directory of this collection is a self-contained WIT project
     * embedded by its own `wasm-tools component embed` run, one after another.
     * Directories are never merged with each other,
     * so packages of different projects never conflict.
     *
     * By default, it contains the `wit` directories of all runtime dependencies of the compilation,
     * followed by the `wit` directory of the project itself,
     * which is therefore embedded last.
     *
     * Directories which do not exist or contain no files are skipped.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val witDirectory: ConfigurableFileCollection

    /**
     * Additional arguments of every `wasm-tools component embed` run.
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

        val inputModule = inputFile.getFile()

        val embedDir = temporaryDir.resolve(EMBED_DIRECTORY_NAME)

        fs.delete {
            it.delete(embedDir)
        }

        val witProjects = witProjects().takeIf { it.isNotEmpty() } ?: return

        val embedded = witProjects.foldIndexed(inputModule) { index: Int, acc: File, witProject: File ->
            val newOutput = embedDir.resolve("$index-${witProject.name}").resolve(inputModule.name)
            newOutput.parentFile.mkdirs()

            logger.debug(
                "Embedding WIT project '{}' into '{}'",
                witProject,
                acc,
            )

            execOperations.exec {
                it.executable = wasmTools
                it.args = listOf(
                    "component",
                    "embed",
                    witProject.absolutePath,
                    acc.absolutePath
                ) +
                        embedArguments.get() +
                        listOf("-o", newOutput.absolutePath)
            }

            newOutput
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

        val component = componentFile.getFile()
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

    /**
     * WIT projects of [witDirectory] which can be passed to `wasm-tools component embed`,
     * in the order they have to be embedded.
     */
    private fun witProjects(): List<File> =
        witDirectory
            .files
            .filter { witProject ->
                val isWitProject = witProject.isDirectory &&
                        witProject.listFilesOrEmpty().any { it.isFile }

                if (!isWitProject) {
                    logger.debug("Skipping '{}' as it is not a WIT project", witProject)
                }

                isWitProject
            }
            .reversed()

    internal companion object {
        /**
         * Default `wasm-tools` executable, resolved from `PATH`.
         */
        const val WASM_TOOLS_EXECUTABLE = "wasm-tools"

        private const val EMBED_DIRECTORY_NAME = "embed"

        internal fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: WasmComponentExec.() -> Unit = {},
        ): TaskProvider<WasmComponentExec> {
            val project = compilation.target.project
            val witDirectories = project.configurations.named(compilation.witConfigurationName)
            return project.registerTask(
                name,
            ) {
                it.executable.convention(WASM_TOOLS_EXECUTABLE)
                it.witDirectory.from(witDirectories)
                it.witDirectory.from(project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME))
                it.configuration()
            }
        }
    }
}

internal const val WIT_DIRECTORY_NAME = "wit"
