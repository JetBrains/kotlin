/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.artifacts.maybeCreateKlibPackingTask
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.named
import java.io.File

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

        /* Apple-specific cinterops can't be produced on non-MacOs machines, so just return an empty dependencies collection */
        if (!compilation.target.crossCompilationOnCurrentHostSupported.getOrThrow()) return@files emptySet<File>()

        (compilation.associatedCompilations + compilation)
            .filterIsInstance<KotlinNativeCompilation>()
            .filter(compilationFilter)
            .map { relevantCompilation -> getAllCInteropOutputFiles(relevantCompilation) }
    }
}

private fun Project.getAllCInteropOutputFiles(compilation: KotlinNativeCompilation): FileCollection {
    val cinteropTasks = compilation.cinterops.map { interop -> interop.interopProcessingTaskName }
        .mapNotNull { taskName ->
            if (taskName in tasks.names) {
                tasks.named<CInteropProcess>(taskName)
            } else null
        }

    if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
        // this part of import isn't ready for working with unpackaged klibs: KTIJ-31053
        return project.filesProvider {
            cinteropTasks.map { interopTask ->
                compilation.maybeCreateKlibPackingTask(
                    interopTask.get().settings.classifier,
                    interopTask.map { it.klibDirectory.get() },
                )
            }
        }
    }
    return project.filesProvider { cinteropTasks.map { it.get().klibFile } }
        .builtBy(*cinteropTasks.toTypedArray())
}
