/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.assembleHierarchicalResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.named

internal fun TaskProvider<PackKotlinArchiveTask>.putKlib(pathProvider: Provider<String>, compileTaskProvider: TaskProvider<out ProducesKlib>) {
    configure { task ->
        task.dependsOn(compileTaskProvider)
        task.into(pathProvider) { spec ->
            // TODO: CC unfriendly
            val compileTask = compileTaskProvider.get()
            if (compileTask.produceUnpackagedKlib.get()) {
                spec.from(compileTask.klibDirectory)
            } else {
                spec.from(task.project.zipTree(compileTask.klibOutput))
            }
        }
    }
}

internal fun TaskProvider<PackKotlinArchiveTask>.fillKotlinArchiveTargetContent(target: KotlinTarget) {
    if (target !is KotlinTargetWithKlibsInKotlinArchiveSupport) return
    // Safe to query because SetupKarArtifactAction invokes this only after AfterFinaliseCompilations.
    if (!target.isStoredInKotlinArchive.get()) return
    val artifactsTasks = when (target) {
        is KotlinNativeTarget -> {
            val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            KotlinArchivePlatformArtifactsTasks(
                mainCompilationTask = mainCompilation.compileTaskProvider,
                cinteropCompilationsTasks = mainCompilation.cinterops.map { target.project.tasks.named<CInteropProcess>(it.interopProcessingTaskName) }
            )
        }
        is KotlinJsIrTarget -> {
            KotlinArchivePlatformArtifactsTasks(
                mainCompilationTask = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider,
                cinteropCompilationsTasks = emptyList()
            )
        }
        else -> return
    }
    putKlib(
        pathProvider = target.kotlinArchivePlatformKlibPath,
        compileTaskProvider = artifactsTasks.mainCompilationTask
    )
    for (cinteropTask in artifactsTasks.cinteropCompilationsTasks) {
        putKlib(
            pathProvider = target.kotlinArchiveCinteropKlibPath(cinteropTask.map { task -> task.outputFileName }),
            compileTaskProvider = cinteropTask
        )
    }
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    target.project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnPublishResources(target) { resources ->
        val assembleOutputDir = mainCompilation.assembleHierarchicalResources(target.targetName, resources)
        configure { task ->
            task.into(target.kotlinArchiveResourcesPath) { spec ->
                spec.from(assembleOutputDir)
            }
        }
    }
}

internal fun TaskProvider<PackKotlinArchiveTask>.fillKotlinArchivePsmContent(project: Project) {
    val psmTask = project.locateOrRegisterGenerateProjectStructureMetadataTask()
    configure { task ->
        task.into(KarLayout.METADATA_DIRECTORY_NAME) { spec ->
            spec.from(psmTask)
        }
    }
}

internal fun TaskProvider<PackKotlinArchiveTask>.fillKotlinArchiveMetadataCompilationContent(compilation: KotlinCompilation<*>) {
    configure { task ->
        task.into("${KarLayout.METADATA_DIRECTORY_NAME}/${compilation.metadataFragmentIdentifier}") { spec ->
            spec.from(compilation.metadataPublishedArtifacts)
        }
    }

    if (compilation is KotlinSharedNativeCompilation) {
        compilation.project.includeCommonizedCInteropMetadata(this, compilation, relativePath = "${KarLayout.METADATA_DIRECTORY_NAME}/")
    }
}
