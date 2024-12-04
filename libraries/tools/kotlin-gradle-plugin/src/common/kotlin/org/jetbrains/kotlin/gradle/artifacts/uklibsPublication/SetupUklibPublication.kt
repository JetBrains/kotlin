/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.uklibsPublication

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Uklib
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Module
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal suspend fun Project.archiveUklibTask(): TaskProvider<ArchiveUklibTask> {
    val taskName = "archiveUklib"
    tasks.locateTask<ArchiveUklibTask>(taskName)?.let { return it }

    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val allTargets = multiplatformExtension.awaitTargets().toMutableList()

    val kgpFragments = kgpFragments(
        metadataTarget = metadataTarget,
        allTargets = allTargets,
    )

    kgpFragments.forEach { fragment ->
        archiveUklib.configure {
            // outputFile might be a directory or a file
            it.inputs.files(fragment.outputFile)
            // some outputFiles are derived from a project.provider, use explicit task dependency
            it.dependsOn(fragment.providingTask)
        }
    }

    archiveUklib.configure {
        it.model.set(
            Uklib(
                module = Module(
                    fragments = kgpFragments.map {
                        it.fragment
                    }.toHashSet(),
                ),
                manifestVersion = Uklib.CURRENT_UMANIFEST_VERSION,
            )
        )
    }

    return archiveUklib
}