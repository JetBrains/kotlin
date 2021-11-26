/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.utils.filesProvider
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

private fun Project.setupCInteropCommonizerDependenciesForCompilation(compilation: KotlinSharedNativeCompilation) {
    val cinteropCommonizerTask = project.commonizeCInteropTask ?: return

    compilation.compileDependencyFiles += filesProvider {
        val cinteropCommonizerDependent = CInteropCommonizerDependent.from(compilation) ?: return@filesProvider emptySet<File>()
        cinteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
    }
}

/**
 * IDE will resolve the dependencies provided on source sets.
 * This will use the [Project.copyCommonizeCInteropForIdeTask] over the regular cinterop commonization task.
 * The copying task prevent red code within the IDE after cleaning the build output.
 */
private fun Project.setupCInteropCommonizerDependenciesForIde(sourceSet: DefaultKotlinSourceSet) {
    val cinteropCommonizerTask = project.copyCommonizeCInteropForIdeTask ?: return

    addIntransitiveMetadataDependencyIfPossible(sourceSet, filesProvider files@{
        val directlyDependent = CInteropCommonizerDependent.from(project, sourceSet)
        val associateDependent = CInteropCommonizerDependent.fromAssociateCompilations(project, sourceSet)

        listOfNotNull(directlyDependent, associateDependent).map { cinteropCommonizerDependent ->
            cinteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
        }
    })
}

private fun Project.setupCInteropTransformCompositeMetadataDependenciesForIde(sourceSet: DefaultKotlinSourceSet) {
    whenEvaluated {
        if (getCommonizerTarget(sourceSet) !is SharedCommonizerTarget) return@whenEvaluated
        addIntransitiveMetadataDependencyIfPossible(
            sourceSet, createCInteropMetadataDependencyClasspathForIde(sourceSet)
        )
    }
}
