/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.archivesName

internal val Project.karPackTask: TaskProvider<PackKotlinArchiveTask>
    get() = project.locateOrRegisterTask<PackKotlinArchiveTask>(KarLayout.PACK_TASK_NAME)

internal val Project.karAssembleTask: TaskProvider<AssembleKotlinArchiveTask>
    get() = project.locateOrRegisterTask<AssembleKotlinArchiveTask>(KarLayout.ASSEMBLE_TASK_NAME)


internal val SetupKotlinArchiveAction = KotlinProjectSetupCoroutine {
    Stage.AfterFinaliseCompilations.await()

    val extension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine

    val kotlinPublicationFormatProvider = extension.publishing.publicationFormat

    val assembleTask = karAssembleTask
    assembleTask.configure { task ->
        task.outputDirectory.set(layout.buildDirectory.dir(KarLayout.ASSEMBLE_DIRECTORY))
        task.onlyIf { kotlinPublicationFormatProvider.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE }
    }
    for (target in extension.awaitTargets()) {
        assembleTask.fillKotlinArchiveTargetContent(target)
    }
    for (compilation in extension.metadataTarget.publishedMetadataCompilations()) {
        assembleTask.fillKotlinArchiveMetadataCompilationContent(compilation)
    }
    assembleTask.fillKotlinArchivePsmContent(project)

    karPackTask.configure { task ->
        task.assembledKarDirectory.set(assembleTask.flatMap { it.outputDirectory })
        task.outputFile.set(
            layout.buildDirectory.file(project.archivesName.map { archivesName ->
                "${KarLayout.PACKING_DIRECTORY}/$archivesName.${KarLayout.KAR_XZ_PACKED_EXTENSION}"
            })
        )
        task.onlyIf { kotlinPublicationFormatProvider.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE }
    }
}
