/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future
import java.io.File

internal fun Project.setupCInteropCommonizerDependencies() {
    val kotlin = this.multiplatformExtensionOrNull ?: return

    kotlin.forAllSharedNativeCompilations { compilation ->
        setupCInteropCommonizerDependenciesForCompilation(compilation)
    }

    kotlin.forAllDefaultKotlinSourceSets { sourceSet ->
        setupCInteropCommonizerDependenciesForIde(sourceSet)
        setupCInteropTransformCompositeMetadataDependenciesForIde(sourceSet)
    }
}

private fun Project.setupCInteropCommonizerDependenciesForCompilation(compilation: KotlinSharedNativeCompilation) = launch {
    val cinteropCommonizerTask = project.commonizeCInteropTask() ?: return@launch

    compilation.compileDependencyFiles += filesProvider {
        val cinteropCommonizerDependent = future { CInteropCommonizerDependent.from(compilation) }.getOrThrow()
            ?: return@filesProvider emptySet<File>()
        cinteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
    }
}

/**
 * IDE will resolve the dependencies provided on source sets.
 * This will use the [Project.copyCommonizeCInteropForIdeTask] over the regular cinterop commonization task.
 * The copying task prevent red code within the IDE after cleaning the build output.
 */
private fun Project.setupCInteropCommonizerDependenciesForIde(sourceSet: DefaultKotlinSourceSet) = launch {
    addIntransitiveMetadataDependencyIfPossible(sourceSet, cinteropCommonizerDependencies(sourceSet))
}

internal fun Project.cinteropCommonizerDependencies(sourceSet: DefaultKotlinSourceSet): FileCollection {
    return filesProvider {
        future {
            val cinteropCommonizerTask = project.copyCommonizeCInteropForIdeTask() ?: return@future project.files()

            val directlyDependent = CInteropCommonizerDependent.from(sourceSet)
            val associateDependent = CInteropCommonizerDependent.fromAssociateCompilations(sourceSet)

            listOfNotNull(directlyDependent, associateDependent).map { cinteropCommonizerDependent ->
                cinteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
            }
        }.getOrThrow()
    }
}

private fun Project.setupCInteropTransformCompositeMetadataDependenciesForIde(sourceSet: DefaultKotlinSourceSet) = launch {
    if (sourceSet.internal.commonizerTarget.await() !is SharedCommonizerTarget) return@launch
    addIntransitiveMetadataDependencyIfPossible(
        sourceSet, createCInteropMetadataDependencyClasspathForIde(sourceSet)
    )
}

