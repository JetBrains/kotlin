/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.cinteropMetadataDirectoryPath
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedOutputDirectory
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.named

// TODO: KT-88605 It should read compilation.output, but it can contain packed klib, which can't distinguish there
private fun Project.klibFileCollection(taskProvider: TaskProvider<*>): FileCollection {
    return project.filesProvider(taskProvider) {
        taskProvider.flatMap { task ->
            if (task !is ProducesKlib) {
                logger.warn("${task.name}, that requested to be stored in Kotlin Archive is expected to produce Klib, falling back to empty")
                project.provider { project.files() }
            } else {
                task.produceUnpackagedKlib.flatMap { isUnpackaged ->
                    if (isUnpackaged) {
                        task.klibDirectory
                    } else {
                        task.klibFile.map { project.zipTree(it) }
                    }
                }
            }
        }
    }
}

internal fun TaskProvider<AssembleKotlinArchiveTask>.fillKotlinArchiveTargetContent(target: KotlinTarget) {
    if (target !is KotlinTargetWithKotlinArchiveSupport) return
    if (!target.isStoredInKotlinArchive.get()) return
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    val pathInKotlinArchive = target.platformNameInKotlinArchive
    configure { task ->
        task.addPlatformKlib(pathInKotlinArchive, task.project.klibFileCollection(mainCompilation.compileTaskProvider))
    }
    if (mainCompilation is KotlinNativeCompilation) {
        for (cinterop in mainCompilation.cinterops) {
            val cinteropTaskProvider = target.project.tasks.named<CInteropProcess>(cinterop.interopProcessingTaskName)
            configure { task ->
                val pathProvider = cinteropTaskProvider.map { "${pathInKotlinArchive}/${it.outputFileName}" }
                val filesProvider = task.project.klibFileCollection(cinteropTaskProvider)
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
