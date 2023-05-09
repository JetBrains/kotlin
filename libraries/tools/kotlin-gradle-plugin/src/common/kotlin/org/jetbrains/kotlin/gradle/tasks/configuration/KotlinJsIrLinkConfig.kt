/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.InvalidUserDataException
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

internal open class KotlinJsIrLinkConfig(
    private val binary: JsIrBinary
) : BaseKotlin2JsCompileConfig<KotlinJsIrLink>(KotlinCompilationInfo(binary.compilation)) {

    private val compilation
        get() = binary.compilation

    init {
        configureTask { task ->
            // Link tasks are not affected by compiler plugin, so set to empty
            task.pluginClasspath.setFrom(objectFactory.fileCollection())

            task.dependsOn(compilation.compileTaskProvider)
            task.dependsOn(compilation.output.classesDirs)
            task.entryModule.fileProvider(
                compilation.output.classesDirs.elements.flatMap {
                    task.project.providers.provider {
                        it.single().asFile
                    }
                }
            ).disallowChanges()
            task.rootCacheDirectory.set(project.layout.buildDirectory.map { it.dir("klib/cache/js/${binary.name}") })
            task.destinationDirectory.convention(
                project.layout.buildDirectory
                    .dir(COMPILE_SYNC)
                    .map { it.dir(compilation.target.targetName) }
                    .map { it.dir(compilation.name) }
                    .map { it.dir(binary.name) }
                    .map { it.dir(NpmProject.DIST_FOLDER) }
            )
            task.compilerOptions.moduleName.convention(project.provider { compilation.npmProject.name })
        }
    }

    override fun configureAdditionalFreeCompilerArguments(
        task: KotlinJsIrLink,
        compilation: KotlinCompilationInfo
    ) {
        task.enhancedFreeCompilerArgs.value(
            task.compilerOptions.freeCompilerArgs.zip(task.modeProperty) { freeArgs, mode ->
                freeArgs.toMutableList().apply {
                    commonJsAdditionalCompilerFlags(compilation)

                    when (mode) {
                        KotlinJsBinaryMode.PRODUCTION -> {
                            configureOptions(
                                compilation,
                                ENABLE_DCE,
                                MINIMIZED_MEMBER_NAMES
                            )
                        }

                        KotlinJsBinaryMode.DEVELOPMENT -> {
                            configureOptions(
                                compilation
                            )
                        }
                        else -> throw InvalidUserDataException(
                            "Unknown KotlinJsBinaryMode to configure the build: $mode"
                        )
                    }

                    val alreadyDefinedOutputMode = any { it.startsWith(PER_MODULE) }
                    if (!alreadyDefinedOutputMode) {
                        add(task.outputGranularity.toCompilerArgument())
                    }
                }
            }
        ).disallowChanges()
    }

    private fun MutableList<String>.configureOptions(
        compilation: KotlinCompilationInfo,
        vararg additionalCompilerArgs: String
    ) {
        additionalCompilerArgs.forEach { arg ->
            if (none { it.startsWith(arg) }) add(arg)
        }

        add(PRODUCE_JS)

        if (compilation.platformType == KotlinPlatformType.wasm) {
            add(WASM_BACKEND)
        }
    }
}