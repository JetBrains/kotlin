/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_JS
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_UNZIPPED_KLIB
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_ZIPPED_KLIB
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import java.io.File

internal typealias Kotlin2JsCompileConfig = BaseKotlin2JsCompileConfig<Kotlin2JsCompile>

internal open class BaseKotlin2JsCompileConfig<TASK : Kotlin2JsCompile>(
    compilation: KotlinCompilationData<*>
) : AbstractKotlinCompileConfig<TASK>(compilation) {

    init {
        val libraryCacheService = project.rootProject.gradle.sharedServices.registerIfAbsent(
            "${Kotlin2JsCompile.LibraryFilterCachingService::class.java.canonicalName}_${Kotlin2JsCompile.LibraryFilterCachingService::class.java.classLoader.hashCode()}",
            Kotlin2JsCompile.LibraryFilterCachingService::class.java
        ) {}

        configureTask { task ->
            task.incremental = propertiesProvider.incrementalJs ?: true
            task.incrementalJsKlib = propertiesProvider.incrementalJsKlib ?: true

            configureAdditionalFreeCompilerArguments(task, compilation)

            @Suppress("DEPRECATION")
            task.compilerOptions.outputFile.convention(
                task.defaultDestinationDirectory.zip(task.enhancedFreeCompilerArgs) { destDir, freeArgs ->
                    val baseName = if (compilation.isMainCompilationData()) {
                        project.name
                    } else {
                        "${project.name}_${compilation.compilationPurpose}"
                    }

                    if (freeArgs.contains(PRODUCE_UNZIPPED_KLIB)) {
                        destDir.asFile.absoluteFile.normalize().absolutePath
                    } else {
                        if (compilation is KotlinJsIrCompilation) {
                            destDir.asFile.resolve("$baseName.$KLIB_TYPE").absoluteFile.normalize().absolutePath
                        } else {
                            val extensionName = if (compilation.platformType == KotlinPlatformType.wasm) ".mjs" else ".js"
                            destDir.asFile.resolve("${compilation.ownModuleName}$extensionName").absolutePath
                        }
                    }
                }
            )

            @Suppress("DEPRECATION")
            task.outputFileProperty.value(
                task.compilerOptions.outputFile.map { File(it) }
            )

            task.destinationDirectory
                .fileProvider(
                    task.outputFileProperty.zip(task.enhancedFreeCompilerArgs) { outputFile, freeArgs ->
                        if (freeArgs.contains(PRODUCE_UNZIPPED_KLIB)) {
                            outputFile
                        } else {
                            outputFile.parentFile
                        }
                    }
                )
                .disallowChanges()

            task.optionalOutputFile.fileProvider(
                task.outputFileProperty.flatMap { outputFile ->
                    task.enhancedFreeCompilerArgs.flatMap { freeArgs ->
                        task.project.providers.provider {
                            outputFile.takeUnless {
                                freeArgs.contains(PRODUCE_UNZIPPED_KLIB)
                            }
                        }
                    }
                }
            ).disallowChanges()
            task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
        }
    }

    protected open fun configureAdditionalFreeCompilerArguments(
        task: TASK,
        compilation: KotlinCompilationData<*>
    ) {
        task.enhancedFreeCompilerArgs.value(
            task.compilerOptions.freeCompilerArgs.map { freeArgs ->
                freeArgs.toMutableList().apply {
                    commonJsAdditionalCompilerFlags(compilation)
                }
            }
        ).disallowChanges()
    }

    protected fun MutableList<String>.commonJsAdditionalCompilerFlags(
        compilation: KotlinCompilationData<*>
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
            val baseName = if (compilation.isMainCompilationData()) {
                project.name
            } else {
                "${project.name}_${compilation.compilationPurpose}"
            }
            add("$MODULE_NAME=${project.klibModuleName(baseName)}")
        }
    }
}