/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForKlibCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.gradle.utils.currentBuildId
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
): FileCollection = getPlatformCinteropOutputsOrEmpty(sourceSet, compilationFilter).asFileCollection(this)

internal fun Project.getPlatformCinteropOutputsOrEmpty(
    sourceSet: DefaultKotlinSourceSet,
    compilationFilter: (KotlinNativeCompilation) -> Boolean = { true },
): List<CInteropOutput> {
    /*
    compatibility metadata variant will still register
    a 'KotlinMetadataCompilation for 'commonMain' which is irrelevant here
    */
    val compilations = sourceSet.internal.compilations
        .filter { compilation -> compilation !is KotlinMetadataCompilation }

    /* Participating in multiple compilations? -> can't propagate -> should be commonized */
    val compilation = compilations.singleOrNull() as? KotlinNativeCompilation ?: return emptyList()

    /* Apple-specific cinterops can't be produced on non-MacOs machines, so just return an empty dependencies collection */
    if (!compilation.target.konanTarget.enabledOnCurrentHostForKlibCompilation(kotlinPropertiesProvider)) return emptyList()

    return (compilation.associatedCompilations + compilation)
        .filterIsInstance<KotlinNativeCompilation>()
        .filter(compilationFilter)
        .flatMap { relevantCompilation -> getAllCInteropOutputs(relevantCompilation) }
        .distinctBy { it.key }
}

internal fun Project.getPropagatedCInteropDependenciesOrEmpty(compilation: KotlinSharedNativeCompilation): FileCollection {
    val compilations = compilation.getImplicitlyDependingNativeCompilations()
    val platformCompilation = compilations.singleOrNull() ?: return files()
    return getAllCInteropOutputs(platformCompilation).asFileCollection(this)
}

private fun Project.getAllCInteropOutputs(compilation: KotlinNativeCompilation): List<CInteropOutput> {
    return compilation.cinterops.mapNotNull { interop ->
        val interopTask = tasks.findByName(interop.interopProcessingTaskName)
        if (interopTask !is CInteropProcess) return@mapNotNull null

        CInteropOutput(
            buildPath = compilation.project.currentBuildId().buildPathCompat,
            projectPath = compilation.project.path,
            targetName = compilation.target.name,
            compilationName = compilation.name,
            cinteropName = interop.name,
            klibLocation = filesProvider(interopTask) { interopTask.outputFileProvider },
        )
    }
}

internal class CInteropOutput(
    val buildPath: String,
    val projectPath: String,
    val targetName: String,
    val compilationName: String,
    val cinteropName: String,
    val klibLocation: FileCollection,
) {
    val key: String get() = "$buildPath/$projectPath/$targetName/$compilationName/$cinteropName"
}

private fun List<CInteropOutput>.asFileCollection(project: Project) = project.filesProvider {
    map { it.klibLocation }
}