/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.addToAssemble
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer.Companion.generateBinaryName
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.subtargets.createDefaultDistribution
import org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinJsIrLinkConfig
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile

interface JsBinary {
    val compilation: KotlinJsCompilation
    val name: String
    val mode: KotlinJsBinaryMode
    val distribution: Distribution
}

sealed class JsIrBinary(
    final override val compilation: KotlinJsIrCompilation,
    final override val name: String,
    override val mode: KotlinJsBinaryMode,
) : JsBinary {
    override val distribution: Distribution =
        createDefaultDistribution(compilation.target.project, compilation.target.targetName, name)

    val linkTaskName: String = linkTaskName()

    val linkSyncTaskName: String = linkSyncTaskName()

    val validateGeneratedTsTaskName: String = validateTypeScriptTaskName()

    @Deprecated(
        "No longer used. To enable TypeScript definitions use generateTypeScriptDefinitions() in the Kotlin JS target instead. Scheduled for removal in Kotlin 2.4.",
        level = DeprecationLevel.ERROR
    )
    var generateTs: Boolean = false

    val outputDirBase: Provider<Directory> = project.layout.buildDirectory
        .dir(COMPILE_SYNC)
        .map { it.dir(compilation.target.targetName) }
        .map { it.dir(compilation.name) }
        .map { it.dir(name) }

    val linkTask: TaskProvider<KotlinJsIrLink> =
        project.registerTask(linkTaskName, KotlinJsIrLink::class.java, listOf(project, target.platformType))

    @Suppress("PropertyName")
    protected val _linkSyncTask: TaskProvider<DefaultIncrementalSyncTask>? =
        if (target.wasmTargetType == KotlinWasmTargetType.WASI) {
            null
        } else {
            project.registerTask<DefaultIncrementalSyncTask>(
                linkSyncTaskName
            ) { task ->
                syncInputConfigure(task)

                task.duplicatesStrategy = DuplicatesStrategy.WARN

                task.from.from(linkSyncTaskRegisteredResources)

                task.destinationDirectory.set(if (target.isNodejsConfigured || target.isBrowserConfigured) compilation.npmProject.dist.mapToFile() else project.layout.buildDirectory.dir(KotlinPlatformType.js.name).map { it.dir(compilation.outputModuleName) }.get().mapToFile())
            }
        }

    internal val defaultLinkSyncTaskInput: Provider<Directory>
        get() = linkTask.flatMap(KotlinJsIrLink::destinationDirectory)

    internal val linkSyncTaskRegisteredResources: TaskProvider<*>
        get() = project.tasks.named(compilation.processResourcesTaskName)

    protected open fun syncInputConfigure(syncTask: DefaultIncrementalSyncTask) {
        syncTask.from.from(defaultLinkSyncTaskInput)
    }

    // Wasi target doesn't have sync task
    // need to extract wasm related binaries
    val linkSyncTask: TaskProvider<DefaultIncrementalSyncTask>
        get() = if (target.wasmTargetType == KotlinWasmTargetType.WASI) {
            throw IllegalStateException("Wasi target has no sync task")
        } else {
            _linkSyncTask!!
        }

    val mainFileName: Provider<String> = linkTask.flatMap {
        it.compilerOptions.moduleName.zip(compilation.fileExtension) { moduleName, extension ->
            "$moduleName.$extension"
        }
    }

    val mainFile: Provider<RegularFile> = linkTask.flatMap {
        it.destinationDirectory.file(mainFileName.get())
    }

    val mainFileSyncPath: Provider<RegularFile> =
        if (target.wasmTargetType == KotlinWasmTargetType.WASI) {
            project.objects.fileProperty()
        } else {
            project.objects.fileProperty().fileProvider(
                linkSyncTask.map {
                    it.destinationDirectory.get().resolve(mainFileName.get())
                }
            )
        }

    private fun linkTaskName(): String =
        lowerCamelCaseName(
            "compile",
            name,
            "Kotlin",
            target.targetName
        )

    private fun linkSyncTaskName(): String =
        lowerCamelCaseName(
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            name,
            COMPILE_SYNC
        )

    private fun validateTypeScriptTaskName(): String =
        lowerCamelCaseName(
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            name,
            TypeScriptValidationTask.NAME
        )

    val target: KotlinJsIrTarget
        get() = compilation.target

    val project: Project
        get() = target.project

    companion object {
        internal fun JsIrBinary.configLinkTask() {
            val configAction = KotlinJsIrLinkConfig(this)
            configAction.configureTask {
                it.libraries.from(project.filesProvider { compilation.runtimeDependencyFiles })
            }
            configAction.configureTask { task ->
                val targetCompilerOptions = compilation.target.compilerOptions
                KotlinJsCompilerOptionsHelper.syncOptionsAsConvention(
                    targetCompilerOptions,
                    task.compilerOptions
                )

                task.modeProperty.set(mode)
                task.dependsOn(compilation.compileTaskProvider)
            }

            configAction.execute(linkTask)
        }
    }
}

@ExperimentalWasmDsl
interface WasmBinary {
    val compilation: KotlinJsIrCompilation

    val name: String

    val outputDirBase: Provider<Directory>

    val mode: KotlinJsBinaryMode

    val linkTask: TaskProvider<KotlinJsIrLink>

    val optimizeTask: TaskProvider<BinaryenExec>
}

internal fun TaskProvider<BinaryenExec>.configureOptimizeTask(binary: WasmBinary) {
    configure { task ->
        val linkTask = binary.linkTask
        val wasmFiles = linkTask.flatMap { link ->
            link.destinationDirectory.locationOnly.map { destDir: Directory ->
                destDir.asFileTree.matching { it.include("**/*.wasm") }
            }
        }

        task.dependsOn(linkTask)
        task.inputFiles.from(wasmFiles)

        val outputDirectory: Provider<Directory> = binary.outputDirBase
            .map { it.dir("optimized") }

        task.outputDirectory.set(outputDirectory)
    }

    val target = binary.compilation.target
    val compilation = binary.compilation

    if (compilation.isMain() && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
        if (target.wasmTargetType == KotlinWasmTargetType.WASI) {
            val project = target.project
            project.addToAssemble(this)
        }
    }
}

open class Executable(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : JsIrBinary(
    compilation,
    name,
    mode
) {
    override val distribution: Distribution =
        createDefaultDistribution(
            compilation.target.project,
            compilation.target.targetName,
            super.distribution.distributionName
        )

    val executeTaskBaseName: String =
        generateBinaryName(
            compilation,
            mode,
            null
        )
}

@ExperimentalWasmDsl
class ExecutableWasm(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : Executable(
    compilation,
    name,
    mode
), WasmBinary {
    override fun syncInputConfigure(syncTask: DefaultIncrementalSyncTask) {
        if (mode == KotlinJsBinaryMode.PRODUCTION) {
            // this is done in optimizeTask "also" block, because optimizeTask cannot be referenced on init stage
        } else {
            super.syncInputConfigure(syncTask)
        }
    }

    override val optimizeTask: TaskProvider<BinaryenExec> = BinaryenExec.register(compilation, optimizeTaskName()) {
        val compileWasmDestDir = linkTask.map {
            it.destinationDirectory
        }
        doLast {
            fs.copy {
                it.from(compileWasmDestDir)
                it.into(outputDirectory)
                it.exclude(inputFiles.map { it.name })
            }
        }
    }.also { binaryenExec ->
        binaryenExec.configureOptimizeTask(this)

        if (mode == KotlinJsBinaryMode.PRODUCTION) {
            _linkSyncTask?.configure {
                it.from.from(binaryenExec.flatMap { it.outputDirectory })
                it.dependsOn(binaryenExec)
            }
        }
    }

    val mainOptimizedFile: Provider<RegularFile> = optimizeTask.flatMap {
        it.outputDirectory.file(mainFileName.get())
    }

    private fun optimizeTaskName(): String =
        "${linkTaskName}Optimize"
}

open class Library(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : JsIrBinary(
    compilation,
    name,
    mode
)

@ExperimentalWasmDsl
class LibraryWasm(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : JsIrBinary(
    compilation,
    name,
    mode
), WasmBinary {
    override fun syncInputConfigure(syncTask: DefaultIncrementalSyncTask) {
        if (mode == KotlinJsBinaryMode.PRODUCTION) {
            syncTask.from.from(optimizeTask.flatMap { it.outputDirectory })
            syncTask.dependsOn(optimizeTask)
        } else {
            super.syncInputConfigure(syncTask)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    override val optimizeTask: TaskProvider<BinaryenExec> = BinaryenExec.register(compilation, optimizeTaskName()) {
        val compileWasmDestDir = linkTask.map {
            it.destinationDirectory
        }

        doLast {
            fs.copy {
                it.from(compileWasmDestDir)
                it.into(outputDirectory)
                it.eachFile {
                    if (it.relativePath.getFile(outputDirectory.get().asFile).exists()) {
                        it.exclude()
                    }
                }
            }
        }
    }.also { binaryenExec ->
        binaryenExec.configureOptimizeTask(this)
    }

    private fun optimizeTaskName(): String =
        "${linkTaskName}Optimize"
}

internal const val COMPILE_SYNC = "compileSync"
