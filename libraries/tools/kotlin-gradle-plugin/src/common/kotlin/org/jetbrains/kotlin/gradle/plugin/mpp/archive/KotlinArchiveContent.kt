/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.crossCompilationSharedData
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.swiftExportMetadataTaskOrNull
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.cinteropMetadataDirectoryPath
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedOutputDirectory
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.named

internal fun TaskProvider<AssembleKotlinArchiveTask>.fillKotlinArchiveTargetContent(target: KotlinTarget) {
    if (target !is KotlinTargetWithKotlinArchiveSupport) return
    if (!target.isStoredInKotlinArchive.get()) return
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    val pathInKotlinArchive = target.platformNameInKotlinArchive
    val platformKlib = target.platformKlibFiles
    val isPlatformKlibPacked = target.doesPlatformKlibRequireUnpacking
    configure { task ->
        task.addPlatformKlib(pathInKotlinArchive, platformKlib, isPlatformKlibPacked)
        task.targetsNotPublishableOnCurrentHost.addAll(
            task.project.provider { if (target.publishable) emptyList() else listOf(target.targetName) }
        )
    }
    if (mainCompilation is KotlinNativeCompilation) {
        val crossCompilationSharedData = mainCompilation.crossCompilationSharedData
        configure { task ->
            task.checkTargetHasNoMissingDependenciesBecauseOfCrossCompilationDisabled(
                targetName = target.targetName,
                crossCompilationData = crossCompilationSharedData,
            )
        }
        for (cinterop in mainCompilation.cinterops) {
            val cinteropTaskProvider = target.project.tasks.named<CInteropProcess>(cinterop.interopProcessingTaskName)
            configure { task ->
                val pathProvider = cinteropTaskProvider.map { "${pathInKotlinArchive}/${it.outputFileName}" }
                val filesProvider = task.project.files(cinteropTaskProvider.flatMap { it.klibDirectory }).builtBy(cinteropTaskProvider)
                task.addCInterop(pathProvider, filesProvider)
            }
        }
    }
}

internal fun TaskProvider<AssembleKotlinArchiveTask>.fillKotlinArchivePsmContent(project: Project) {
    val psmTask = project.locateOrRegisterGenerateProjectStructureMetadataTask()
    configure { task ->
        task.projectStructureMetadataFile.fileProvider(psmTask.map { it.resultFile })
    }
}

/**
 * Stores the Swift Export metadata json at [KarLayout.SWIFT_EXPORT_METADATA_FILE_PATH] if the project has one.
 */
internal fun TaskProvider<AssembleKotlinArchiveTask>.fillKotlinArchiveSwiftExportMetadataContent(project: Project) {
    val serializeTask = project.swiftExportMetadataTaskOrNull() ?: return
    configure { task ->
        task.swiftExportMetadataFile.set(serializeTask.map { it.metadataFile.get() })
    }
}

internal suspend fun TaskProvider<AssembleKotlinArchiveTask>.fillKotlinArchiveMetadataCompilationContent(compilation: KotlinCompilation<*>) {
    configure { task ->
        task.addMetadataKlib(compilation.metadataFragmentIdentifier, compilation.metadataPublishedArtifacts)
    }

    if (compilation is KotlinSharedNativeCompilation) {
        CInteropCommonizerDependent.from(compilation)?.let { commonizerDependencyToken ->
            val commonizeTask = compilation.project.commonizeCInteropTask() ?: return@let
            val directory = compilation.project.commonizedOutputDirectory(commonizerDependencyToken) ?: return@let
            configure { task ->
                task.addMetadataKlib(
                    compilation.cinteropMetadataDirectoryPath(),
                    compilation.project.project.files(directory).builtBy(commonizeTask)
                )
            }
        }
    }
}
