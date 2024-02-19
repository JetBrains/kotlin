/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer.Companion.generateBinaryName
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.subtargets.createDefaultDistribution
import org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
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

    var generateTs: Boolean = false

    val linkTask: TaskProvider<KotlinJsIrLink> = project.registerTask(linkTaskName, KotlinJsIrLink::class.java, listOf(project))

    private val _linkSyncTask: TaskProvider<DefaultIncrementalSyncTask>? =
        if (target.wasmTargetType == KotlinWasmTargetType.WASI) {
            null
        } else {
            project.registerTask<DefaultIncrementalSyncTask>(
                linkSyncTaskName
            ) { task ->
                task.from.from(
                    linkTask.flatMap { linkTask ->
                        linkTask.destinationDirectory
                    }
                )

                task.from.from(project.tasks.named(compilation.processResourcesTaskName))

                task.destinationDirectory.set(compilation.npmProject.dist.mapToFile())
            }
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
        get() = compilation.target as KotlinJsIrTarget

    val project: Project
        get() = target.project

    companion object {
        internal fun JsIrBinary.configLinkTask() {
            val configAction = KotlinJsIrLinkConfig(this)
            configAction.configureTask {
                it.libraries.from(project.filesProvider { compilation.runtimeDependencyFiles })
            }
            configAction.configureTask { task ->
                val targetCompilerOptions = (compilation.target as KotlinJsIrTarget).compilerOptions
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

open class ExecutableWasm(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : Executable(
    compilation,
    name,
    mode
) {
    val optimizeTaskName: String = optimizeTaskName()

    val optimizeTask: TaskProvider<BinaryenExec> = BinaryenExec.create(compilation, optimizeTaskName) {
        val compileWasmDestDir = linkTask.map {
            it.destinationDirectory
        }

        val compiledWasmFile = linkTask.flatMap { link ->
            link.destinationDirectory.locationOnly.zip(link.compilerOptions.moduleName) { destDir, moduleName ->
                destDir.file("$moduleName.wasm")
            }
        }

        dependsOn(linkTask)
        inputFileProperty.set(compiledWasmFile)

        val outputDirectory: Provider<Directory> = target.project.layout.buildDirectory
            .dir(COMPILE_SYNC)
            .map { it.dir(compilation.target.targetName) }
            .map { it.dir(compilation.name) }
            .map { it.dir(name) }
            .map { it.dir("optimized") }

        this.outputDirectory.set(outputDirectory)

        outputFileName.set(
            compiledWasmFile.map { it.asFile.name }
        )

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
    }

    private fun optimizeTaskName(): String =
        "${linkTaskName}Optimize"
}

class Library(
    compilation: KotlinJsIrCompilation,
    name: String,
    mode: KotlinJsBinaryMode,
) : JsIrBinary(
    compilation,
    name,
    mode
)

internal const val COMPILE_SYNC = "compileSync"