/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager.CompilationModule.Type.Auxiliary
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager.CompilationModule.Type.Main
import org.jetbrains.kotlin.gradle.utils.ObservableSet

internal interface InternalKotlinCompilation<out T : KotlinCommonOptions> : KotlinCompilation<T> {
    override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
    val friendPaths: Iterable<FileCollection>

    // TODO NOW: Remove default impl
    val compilationModule: KotlinCompilationModuleManager.CompilationModule
        get() = KotlinCompilationModuleManager.CompilationModule(
            compilationName = compilationName,
            ownModuleName = project.provider { (this as AbstractKotlinCompilation<*>).ownModuleName },
            type = if (isMain()) Main else Auxiliary
        )

    // TODO NOW: Remove default impl
    override val runtimeDependencyFiles: FileCollection? get() = null
    override val runtimeDependencyConfigurationName: String? get() = null
    val processResourcesTaskName: String? get() = null

}

internal val <T : KotlinCommonOptions> KotlinCompilation<T>.internal: InternalKotlinCompilation<T>
    get() = (this as? InternalKotlinCompilation<T>) ?: throw IllegalArgumentException(
        "KotlinCompilation($name) ${this::class} does not implement ${InternalKotlinCompilation::class}"
    )