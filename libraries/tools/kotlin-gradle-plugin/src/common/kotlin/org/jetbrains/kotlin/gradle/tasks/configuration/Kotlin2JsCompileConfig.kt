/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.incremental.IncrementalModuleInfoBuildService
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

internal typealias Kotlin2JsCompileConfig = BaseKotlin2JsCompileConfig<Kotlin2JsCompile>

internal open class BaseKotlin2JsCompileConfig<TASK : Kotlin2JsCompile>(
    compilation: KotlinCompilationInfo,
) : AbstractKotlinCompileConfig<TASK>(compilation) {

    init {
        val libraryFilterCachingService = LibraryFilterCachingService.registerIfAbsent(project)

        val incrementalModuleInfoProvider = IncrementalModuleInfoBuildService.registerIfAbsent(
            project,
            objectFactory.providerWithLazyConvention { GradleCompilerRunner.buildModulesInfo(project.gradle) },
        )

        configureTask { task ->
            task.incremental = propertiesProvider.incrementalJs ?: true
            task.incrementalJsKlib = propertiesProvider.incrementalJsKlib ?: true

            configureAdditionalFreeCompilerArguments(task, compilation)

            task.libraryFilterCacheService.value(libraryFilterCachingService).disallowChanges()
            task.incrementalModuleInfoProvider.value(incrementalModuleInfoProvider).disallowChanges()
        }
    }

    protected open fun configureAdditionalFreeCompilerArguments(
        task: TASK,
        compilation: KotlinCompilationInfo,
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
        compilation: KotlinCompilationInfo,
    ) {
        // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
        val baseName = if (compilation.isMain) {
            project.name
        } else {
            "${project.name}_${compilation.compilationName}"
        }
        if (none { it.startsWith(KLIB_MODULE_NAME) }) {
            add("$KLIB_MODULE_NAME=${project.klibModuleName(baseName)}")
        }

        if (compilation.platformType == KotlinPlatformType.wasm) {
            add(WASM_BACKEND)
            val wasmTargetType = ((compilation.origin as KotlinJsIrCompilation).target as KotlinJsIrTarget).wasmTargetType!!
            val targetValue = if (wasmTargetType == KotlinWasmTargetType.WASI) "wasm-wasi" else "wasm-js"
            add("$WASM_TARGET=$targetValue")
        }
    }
}