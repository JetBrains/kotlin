/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.project.model.LanguageSettings
import java.io.File

internal typealias Kotlin2JsCompileConfig = BaseKotlin2JsCompileConfig<Kotlin2JsCompile>

internal open class BaseKotlin2JsCompileConfig<TASK : Kotlin2JsCompile> : AbstractKotlinCompileConfig<TASK> {

    init {
        val libraryCacheService = project.rootProject.gradle.sharedServices.registerIfAbsent(
            "${Kotlin2JsCompile.LibraryFilterCachingService::class.java.canonicalName}_${Kotlin2JsCompile.LibraryFilterCachingService::class.java.classLoader.hashCode()}",
            Kotlin2JsCompile.LibraryFilterCachingService::class.java
        ) {}

        configureTask { task ->
            task.incrementalJs = propertiesProvider.incrementalJs ?: true
            task.incrementalJsKlib = propertiesProvider.incrementalJsKlib ?: true
            task.incremental.value(true)

            if (propertiesProvider.useK2 == true) {
                task.kotlinOptions.useK2 = true
            }

            task.destinationDirectory
                .convention(
                    project.objects.directoryProperty().fileProvider(
                        task.defaultDestinationDirectory.map {
                            val freeArgs = task.enhancedFreeCompilerArgs.get()
                            if (task.compilerOptions.outputFile.orNull != null) {
                                if (freeArgs.contains(PRODUCE_UNZIPPED_KLIB)) {
                                    val file = File(task.compilerOptions.outputFile.get())
                                    if (file.extension == "") file else file.parentFile
                                } else {
                                    File(task.compilerOptions.outputFile.get()).parentFile
                                }
                            } else {
                                it.asFile
                            }
                        }
                    )
                )

            task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
        }
    }

    constructor(compilation: KotlinCompilationInfo) : super(compilation) {
        configureTask { task ->
            configureAdditionalFreeCompilerArguments(task, compilation)

            task.compilerOptions.moduleName.convention(compilation.moduleName)

            configureOutputFileProperty(task, project.provider { compilation.platformType.fileExtension })
        }
    }

    constructor(project: Project, ext: KotlinTopLevelExtension) : super(
        project, ext, languageSettings = getDefaultLangSetting(project, ext)
    ) {
        configureTask { task ->
            // TODO: maybe expose compilation name in task or compiler options.
            val isMain = project.provider { task.sourceSetName.get() == "main" }
            configureAdditionalFreeCompilerArguments(task, isMain, task.sourceSetName)

            // Module name is required from user.

            val fileExtension =
                task.compilerOptions.freeCompilerArgs.map {
                    if (it.contains(WASM_BACKEND)) {
                        ".mjs"
                    } else {
                        ".js"
                    }
                }
            configureOutputFileProperty(task, fileExtension)
        }
    }

    private fun configureOutputFileProperty(task: TASK, fileExtension: Provider<String>) {
        @Suppress("DEPRECATION")
        task.outputFileProperty.value(
            task.destinationDirectory.flatMap { dir ->
                if (task.compilerOptions.outputFile.orNull != null) {
                    task.compilerOptions.outputFile.map { File(it) }
                } else {
                    task.compilerOptions.moduleName.map { name ->
                        dir.file(name + fileExtension.get()).asFile
                    }
                }
            }
        )
    }

    private fun configureAdditionalFreeCompilerArguments(
        task: TASK,
        isMain: Provider<Boolean>,
        compilationName: Provider<String>
    ) {
        task.enhancedFreeCompilerArgs.value(
            task.compilerOptions.freeCompilerArgs.map { freeArgs ->
                freeArgs.toMutableList().apply {
                    commonJsAdditionalCompilerFlags(isMain, compilationName)
                }
            }
        ).disallowChanges()
    }

    protected open fun configureAdditionalFreeCompilerArguments(
        task: TASK,
        compilation: KotlinCompilationInfo
    ) {
        configureAdditionalFreeCompilerArguments(
            task,
            project.provider { compilation.isMain },
            project.provider { compilation.compilationName }
        )
    }

    protected fun MutableList<String>.commonJsAdditionalCompilerFlags(
        compilation: KotlinCompilationInfo
    ) {
        commonJsAdditionalCompilerFlags(
            project.provider { compilation.isMain },
            project.provider { compilation.compilationName }
        )
    }

    private fun MutableList<String>.commonJsAdditionalCompilerFlags(
        isMain: Provider<Boolean>,
        compilationName: Provider<String>
    ) {
        if (contains(DISABLE_PRE_IR) &&
            !contains(PRODUCE_UNZIPPED_KLIB) &&
            !contains(PRODUCE_ZIPPED_KLIB)
        ) {
            add(PRODUCE_UNZIPPED_KLIB)
        }

        if (contains(PRODUCE_JS) ||
            contains(PRODUCE_UNZIPPED_KLIB) ||
            contains(PRODUCE_ZIPPED_KLIB)
        ) {
            // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
            val baseName = if (isMain.get()) {
                project.name
            } else {
                "${project.name}_${compilationName.get()}"
            }
            if (none { it.startsWith(KLIB_MODULE_NAME) }) {
                add("$KLIB_MODULE_NAME=${project.klibModuleName(baseName)}")
            }
        }
    }
}