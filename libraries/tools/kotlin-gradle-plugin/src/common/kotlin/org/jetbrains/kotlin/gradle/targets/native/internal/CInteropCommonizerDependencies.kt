/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future
import java.io.File

internal fun Project.setupCInteropCommonizerDependencies() {
    val kotlin = this.multiplatformExtensionOrNull ?: return

    kotlin.forAllSharedNativeCompilations { compilation ->
        setupCInteropCommonizerDependenciesForCompilation(compilation)
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

internal fun Project.cinteropCommonizerDependenciesForIde(sourceSet: KotlinSourceSet): FileCollection {
    return filesProvider {
        future {
            val cinteropCommonizerTask = project.copyCommonizeCInteropForIdeTask()
            cinteropCommonizerDependencies(sourceSet, cinteropCommonizerTask)
        }.getOrThrow()
    }
}

internal suspend fun Project.cinteropCommonizerDependencies(
    sourceSet: KotlinSourceSet,
    cInteropCommonizerTask: TaskProvider<out AbstractCInteropCommonizerTask>? = null,
): FileCollection {
    if (cInteropCommonizerTask == null) return project.files()

    val directlyDependent = CInteropCommonizerDependent.from(sourceSet)
    val associateDependent = CInteropCommonizerDependent.fromAssociateCompilations(sourceSet)

    return files(
        listOfNotNull(directlyDependent, associateDependent).map { cinteropCommonizerDependent ->
            cInteropCommonizerTask.get().commonizedOutputLibraries(cinteropCommonizerDependent)
        }
    )
}
