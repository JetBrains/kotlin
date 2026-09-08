/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future

internal fun Project.setupCInteropCommonizerDependencies() {
    val kotlin = this.multiplatformExtensionOrNull ?: return

    kotlin.forAllSharedNativeCompilations { compilation ->
        setupCInteropCommonizerDependenciesForCompilation(compilation)
    }
}

private fun Project.setupCInteropCommonizerDependenciesForCompilation(compilation: KotlinSharedNativeCompilation) = launch {
    val cinteropCommonizerDependent = CInteropCommonizerDependent.from(compilation) ?: return@launch
    val commonizedCInterops = commonizedOutputLibraries(cinteropCommonizerDependent) ?: return@launch
    compilation.compileDependencyFiles += commonizedCInterops
}

internal fun Project.cinteropCommonizerDependenciesForIde(sourceSet: KotlinSourceSet): FileCollection {
    return filesProvider {
        future {
            files(cinteropCommonizerDependents(sourceSet).mapNotNull { commonizedOutputLibrariesForIde(it) })
        }.getOrThrow()
    }
}

internal suspend fun Project.cinteropCommonizerDependencies(sourceSet: KotlinSourceSet): FileCollection =
    files(cinteropCommonizerDependents(sourceSet).mapNotNull { commonizedOutputLibraries(it) })

private suspend fun cinteropCommonizerDependents(sourceSet: KotlinSourceSet): List<CInteropCommonizerDependent> =
    listOfNotNull(
        CInteropCommonizerDependent.from(sourceSet),
        CInteropCommonizerDependent.fromAssociateCompilations(sourceSet),
    )
