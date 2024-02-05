/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

/**
 * Will propagate "original"/"platform" cinterops to intermediate source sets
 * and 'shared native' compilations if necessary.
 *
 * cinterops will be forwarded when a source set/ compilation has just a single platform
 * dependee
 *
 * e.g.
 *
 * ```
 * kotlin {
 *      sourceSets {
 *          val nativeMain by sourceSets.creating
 *          val linuxX64Main by sourceSets.getting
 *          linuxX64Main.dependsOn(nativeMain)
 *      }
 * }
 * ```
 *
 * In this example 'nativeMain' has only a single native
 * target and a single native source set depending on it.
 * All cinterops defined on linuxX64's main compilation shall be propagated
 * to the 'nativeMain' source set (and its 'shared native' compilation) if it exists.
 */
internal fun Project.setupCInteropPropagatedDependencies() {
    val kotlin = this.multiplatformExtensionOrNull ?: return

    kotlin.forAllSharedNativeCompilations { compilation ->
        compilation.compileDependencyFiles += getPropagatedCInteropDependenciesOrEmpty(compilation)
    }

    kotlin.forAllDefaultKotlinSourceSets { sourceSet ->
        addIntransitiveMetadataDependencyIfPossible(
            sourceSet, getPropagatedCInteropDependenciesOrEmpty(sourceSet)
        )
    }
}

internal fun Project.getPropagatedCInteropDependenciesOrEmpty(sourceSet: DefaultKotlinSourceSet): FileCollection =
    getPlatformCinteropDependenciesOrEmpty(sourceSet) { relevantCompilation ->
        /* Source Set is directly included in compilation -> No need to add dependency again (when looking for propagated dependencies) */
        sourceSet !in relevantCompilation.kotlinSourceSets
    }

internal fun Project.getPlatformCinteropDependenciesOrEmpty(
    sourceSet: DefaultKotlinSourceSet,
    compilationFilter: (KotlinNativeCompilation) -> Boolean = { true },
): FileCollection {
    return filesProvider files@{
        /*
        compatibility metadata variant will still register
        a 'KotlinMetadataCompilation for 'commonMain' which is irrelevant here
        */
        val compilations = sourceSet.internal.compilations
            .filter { compilation -> compilation !is KotlinMetadataCompilation }

        /* Participating in multiple compilations? -> can't propagate -> should be commonized */
        val compilation = compilations.singleOrNull() as? KotlinNativeCompilation ?: return@files emptySet<File>()

        (compilation.associatedCompilations + compilation)
            .filterIsInstance<KotlinNativeCompilation>()
            .filter(compilationFilter)
            .map { relevantCompilation -> getAllCInteropOutputFiles(relevantCompilation) }
    }
}

private fun Project.getPropagatedCInteropDependenciesOrEmpty(compilation: KotlinSharedNativeCompilation) = filesProvider files@{
    val compilations = compilation.getImplicitlyDependingNativeCompilations()
    val platformCompilation = compilations.singleOrNull() ?: return@files emptySet<File>()
    getAllCInteropOutputFiles(platformCompilation)
}

private fun Project.getAllCInteropOutputFiles(compilation: KotlinNativeCompilation): FileCollection {
    val cinteropTasks = compilation.cinterops.map { interop -> interop.interopProcessingTaskName }
        .mapNotNull { taskName -> tasks.findByName(taskName) as? CInteropProcess }

    return project.filesProvider { cinteropTasks.map { it.outputFileProvider } }
        .builtBy(*cinteropTasks.toTypedArray())
}
