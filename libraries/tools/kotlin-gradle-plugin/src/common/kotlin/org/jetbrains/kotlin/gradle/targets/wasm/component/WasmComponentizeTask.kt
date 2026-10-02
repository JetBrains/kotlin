/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeEnv
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.WasmToolsPlugin
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.gradle.utils.mapOrNull
import java.io.File
import java.net.URI
import javax.inject.Inject

/**
 * Produces a WebAssembly component out of a core Wasm module using `wasm-tools`.
 *
 * Every WIT project of [witProjects] is embedded into the module by its own
 * `wasm-tools component embed` run, each run taking the result of the previous one as its input,
 * and the last result is converted into a component by a single `wasm-tools component new` run:
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
 */
@ExperimentalWasmDsl
@DisableCachingByDefault
abstract class WasmComponentizeTask
internal constructor() : DefaultTask() {
    @get:Inject
    internal abstract val execOperations: ExecOperations

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:Input
    abstract val executable: Property<String>

    @get:Internal
    internal abstract val env: Property<WasmtimeEnv>

    @get:Input
    internal val allowInsecureProtocol: Provider<Boolean> = env.map {
        it.allowInsecureProtocol
    }

    @get:Input
    @get:Optional
    internal val downloadBaseUrlProvider: Provider<String> = env.mapOrNull(project.providers) {
        it.downloadBaseUrl
    }

    @get:Input
    internal val ivyDependencyProvider: Provider<String> = env.map { it.ivyDependency }

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
    abstract val witProjects: ConfigurableFileCollection

    /**
     * Additional arguments of every `wasm-tools component embed` run.
     */
    @get:Input
    abstract val componentEmbedArguments: ListProperty<String>

    /**
     * Additional arguments of the `wasm-tools component new` command.
     */
    @get:Input
    abstract val componentNewArguments: ListProperty<String>

    /**
     * Resulting Wasm component.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract var adapter: Provider<Configuration>

    @get:Classpath
    @get:Optional
    internal val dist: File by lazy {
        withUrlRepo {
            adapter.get().files.single()
        }
    }

    @TaskAction
    fun componentize() {
        val wasmTools = executable.get()

        val inputModule = inputFile.getFile()

        val embedDir = temporaryDir.resolve(EMBED_DIRECTORY_NAME)

        fs.delete {
            it.delete(embedDir)
        }

        val embedded = resolvedWitProjects().foldIndexed(inputModule) { index: Int, acc: File, witProject: File ->
            val newOutput = embedDir
                .resolve("$index-${witProject.parentFile.name}-${witProject.name}")
                .resolve(inputModule.name)
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
                        componentEmbedArguments.get() +
                        listOf("-o", newOutput.absolutePath)
            }

            newOutput
        }

        val adaptFile = temporaryDir
            .resolve("wasi_snapshot_preview1.command.wasm")
            .also {
                dist.copyTo(it, overwrite = true)
            }

        val componentDirectory = outputDirectory.getFile()
        componentDirectory.mkdirs()
        val component = componentDirectory.resolve(inputModule.name)
        execOperations.exec {
            it.executable = wasmTools
            it.args = listOf(
                "component",
                "new",
                embedded.absolutePath,
                "--adapt",
                "wasi_snapshot_preview1=${adaptFile.absolutePath}",
            ) +
                    componentNewArguments.get() +
                    listOf("-o", component.absolutePath)
        }
    }

    /**
     * WIT projects of [witProjects] which can be passed to `wasm-tools component embed`,
     * in the order they have to be embedded.
     *
     * However, the order doesn't matter because each step of embed is independent.
     */
    private fun resolvedWitProjects(): List<File> =
        witProjects
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

    private fun <T> withUrlRepo(action: () -> T): T {
        val repo = downloadBaseUrlProvider.orNull?.let {
            project.repositories.ivy { repo ->
                repo.name = "Distributions at $it"
                repo.url = URI(it)

                repo.isAllowInsecureProtocol = allowInsecureProtocol.get()

                repo.patternLayout {
                    it.artifact("v[revision]/[artifact].[ext]")
                }
                repo.metadataSources { it.artifact() }
                repo.content { it.includeModule("bytecodealliance.wasmtime", "wasi_snapshot_preview1.command") }
            }
        }

        return action().also {
            repo?.let { project.repositories.remove(it) }
        }
    }

    internal companion object {
        private const val EMBED_DIRECTORY_NAME = "embed"

        internal fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: WasmComponentizeTask.() -> Unit = {},
        ): TaskProvider<WasmComponentizeTask> {
            val project = compilation.target.project
            val witDirectories = project.configurations.named(compilation.witConfigurationName)
            val wasmTools = WasmToolsPlugin.applyWithEnvSpec(project)
            return project.registerTask(
                name,
            ) {
                it.executable.convention(wasmTools.executable)
                with(wasmTools) {
                    it.dependsOn(project.wasmToolsSetupTaskProvider)
                }
                it.witProjects.from(project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME))
                it.witProjects.from(witDirectories)
                it.configuration()
            }
        }
    }
}

/**
 * Name of the directory with WIT declarations, both inside a klib and in a project.
 */
internal const val WIT_DIRECTORY_NAME = "wit"
