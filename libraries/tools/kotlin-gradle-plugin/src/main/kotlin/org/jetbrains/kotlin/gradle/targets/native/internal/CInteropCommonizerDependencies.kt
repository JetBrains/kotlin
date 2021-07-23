/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal fun Project.setupCInteropCommonizerDependencies() {
    val kotlin = this.multiplatformExtensionOrNull ?: return

    kotlin.targets.withType(KotlinMetadataTarget::class.java).all { target ->
        target.compilations.withType(KotlinSharedNativeCompilation::class.java).all { compilation ->
            setupCInteropCommonizerDependenciesForCompilation(compilation)
        }
    }

    kotlin.sourceSets.withType(DefaultKotlinSourceSet::class.java).all { sourceSet ->
        setupCInteropCommonizerDependenciesForIde(sourceSet)
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

    addDependency(sourceSet, filesProvider files@{
        val sourceSetCommonizerTarget = getCommonizerTarget(sourceSet)

        val additionalVisibleSourceSets = sourceSet.getAdditionalVisibleSourceSets()
            .filter { sourceSet -> getCommonizerTarget(sourceSet) == sourceSetCommonizerTarget }

        val cinteropCommonizerDependents = (additionalVisibleSourceSets + sourceSet).toSet()
            .mapNotNull { sourceSet -> CInteropCommonizerDependent.from(this, sourceSet) }
            .toSet()

        cinteropCommonizerDependents.map { cinteropCommonizerDependent ->
            cinteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
        }
    })
}

/**
 * Dependencies here are using a special configuration called 'intransitiveMetadataConfiguration'.
 * This special configuration can tell the IDE that this dependencies shall *not* be transitively be visible
 * to dependsOn edges. This is necessary for the way the commonizer handles it's "expect refinement" approach.
 * In this mode, every source set will receive exactly one commonized library to analyze its source code with.
 */
private fun Project.addDependency(sourceSet: DefaultKotlinSourceSet, dependency: FileCollection) {
    val dependencyConfigurationName =
        if (project.isIntransitiveMetadataConfigurationEnabled) sourceSet.intransitiveMetadataConfigurationName
        else sourceSet.implementationMetadataConfigurationName
    project.dependencies.add(dependencyConfigurationName, dependency)
}
