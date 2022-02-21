/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.targets.js.ir.isProduceUnzippedKlib
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
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

            task.outputFileProperty.value(task.project.provider {
                task.kotlinOptions.outputFile?.let(::File)
                    ?: task.destinationDirectory.locationOnly.get().asFile.resolve("${compilation.ownModuleName}.js")
            }).disallowChanges()

            task.optionalOutputFile.fileProvider(task.outputFileProperty.flatMap { outputFile ->
                task.project.provider {
                    outputFile.takeUnless { task.kotlinOptions.isProduceUnzippedKlib() }
                }
            }).disallowChanges()
            task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
        }
    }
}